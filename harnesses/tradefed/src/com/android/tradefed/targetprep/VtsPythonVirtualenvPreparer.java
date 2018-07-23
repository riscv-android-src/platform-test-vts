/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tradefed.targetprep;

import com.android.annotations.VisibleForTesting;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.command.remote.DeviceDescriptor;
import com.android.tradefed.config.Option;
import com.android.tradefed.config.OptionClass;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.invoker.IInvocationContext;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.targetprep.multi.IMultiTargetPreparer;
import com.android.tradefed.util.CommandResult;
import com.android.tradefed.util.CommandStatus;
import com.android.tradefed.util.EnvUtil;
import com.android.tradefed.util.FileUtil;
import com.android.tradefed.util.IRunUtil;
import com.android.tradefed.util.RunUtil;
import com.android.tradefed.util.VtsFileUtil;
import com.android.tradefed.util.VtsPythonRunnerHelper;
import com.android.tradefed.util.VtsVendorConfigFileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Sets up a Python virtualenv on the host and installs packages. To activate it, the working
 * directory is changed to the root of the virtualenv.
 *
 * This's a fork of PythonVirtualenvPreparer and is forked in order to simplify the change
 * deployment process and reduce the deployment time, which are critical for VTS services.
 * That means changes here will be upstreamed gradually.
 */
@OptionClass(alias = "python-venv")
public class VtsPythonVirtualenvPreparer implements IMultiTargetPreparer {
    private static final String LOCAL_PYPI_PATH_ENV_VAR_NAME = "VTS_PYPI_PATH";
    private static final String LOCAL_PYPI_PATH_KEY = "pypi_packages_path";
    private static final int BASE_TIMEOUT = 1000 * 60;
    public static final String VIRTUAL_ENV_V3 = "VIRTUAL_ENV_V3";
    public static final String VIRTUAL_ENV = "VIRTUAL_ENV";

    @Option(name = "venv-dir", description = "path of an existing virtualenv to use")
    protected File mVenvDir = null;

    @Option(name = "requirements-file", description = "pip-formatted requirements file")
    private File mRequirementsFile = null;

    @Option(name = "script-file", description = "scripts which need to be executed in advance")
    private Collection<String> mScriptFiles = new TreeSet<>();

    @Option(name = "dep-module", description = "modules which need to be installed by pip")
    protected Collection<String> mDepModules = new LinkedHashSet<>();

    @Option(name = "no-dep-module", description = "modules which should not be installed by pip")
    private Collection<String> mNoDepModules = new TreeSet<>();

    @Option(name = "reuse",
            description = "Reuse an exising virtualenv path if exists in "
                    + "temp directory. When this option is enabled, virtualenv directory used or "
                    + "created by this preparer will not be deleted after tests complete.")
    protected boolean mReuse = false;

    @Option(name = "python-version",
            description = "The version of a Python interpreter to use."
                    + "Currently, only major version number is fully supported."
                    + "Example: \"2\", or \"3\".")
    private String mPythonVersion = "2";

    private IBuildInfo mBuildInfo = null;
    private DeviceDescriptor mDescriptor = null;
    private IRunUtil mRunUtil = new RunUtil();

    String mLocalPypiPath = null;
    String mPipPath = null;

    // Since we allow virtual env path to be reused during a test plan/module, only the preparer
    // which created the directory should be the one to delete it.
    private boolean mIsDirCreator = false;

    // If the same object is used in multiple threads (in sharding mode), the class
    // needs to know when it is safe to call the teardown method.
    private int mNumOfInstances = 0;

    // A map of initially installed pip modules and versions. Newly installed modules are not
    // currently added automatically.
    private Map<String, String> mPipInstallList = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void setUp(IInvocationContext context)
            throws TargetSetupError, BuildError, DeviceNotAvailableException {
        ++mNumOfInstances;
        mBuildInfo = context.getBuildInfos().get(0);
        if (mNumOfInstances == 1) {
            CLog.i("Preparing python dependencies...");
            ITestDevice device = context.getDevices().get(0);
            mDescriptor = device.getDeviceDescriptor();
            createVirtualenv(mBuildInfo);
            CLog.d("Python virtualenv path is: " + mVenvDir);
            VtsPythonRunnerHelper.activateVirtualenv(getRunUtil(), mVenvDir.getAbsolutePath());
            setLocalPypiPath();
            installDeps();
        }
        addPathToBuild(mBuildInfo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void tearDown(IInvocationContext context, Throwable e)
            throws DeviceNotAvailableException {
        --mNumOfInstances;
        if (mNumOfInstances > 0) {
            // Since this is a host side preparer, no need to repeat
            return;
        }
        if (!mReuse && mVenvDir != null && mIsDirCreator) {
            try {
                recursiveDelete(mVenvDir.toPath());
                CLog.d("Deleted the virtual env's temp working dir, %s.", mVenvDir);
            } catch (IOException exception) {
                CLog.e("Failed to delete %s: %s", mVenvDir, exception);
            }
            mVenvDir = null;
        }
    }

    /**
     * This method sets mLocalPypiPath, the local PyPI package directory to
     * install python packages from in the installDeps method.
     */
    protected void setLocalPypiPath() {
        VtsVendorConfigFileUtil configReader = new VtsVendorConfigFileUtil();
        if (configReader.LoadVendorConfig(mBuildInfo)) {
            // First try to load local PyPI directory path from vendor config file
            try {
                String pypiPath = configReader.GetVendorConfigVariable(LOCAL_PYPI_PATH_KEY);
                if (pypiPath.length() > 0 && dirExistsAndHaveReadAccess(pypiPath)) {
                    mLocalPypiPath = pypiPath;
                    CLog.d(String.format("Loaded %s: %s", LOCAL_PYPI_PATH_KEY, mLocalPypiPath));
                }
            } catch (NoSuchElementException e) {
                /* continue */
            }
        }

        // If loading path from vendor config file is unsuccessful,
        // check local pypi path defined by LOCAL_PYPI_PATH_ENV_VAR_NAME
        if (mLocalPypiPath == null) {
            CLog.d("Checking whether local pypi packages directory exists");
            String pypiPath = System.getenv(LOCAL_PYPI_PATH_ENV_VAR_NAME);
            if (pypiPath == null) {
                CLog.d("Local pypi packages directory not specified by env var %s",
                        LOCAL_PYPI_PATH_ENV_VAR_NAME);
            } else if (dirExistsAndHaveReadAccess(pypiPath)) {
                mLocalPypiPath = pypiPath;
                CLog.d("Set local pypi packages directory to %s", pypiPath);
            }
        }

        if (mLocalPypiPath == null) {
            CLog.d("Failed to set local pypi packages path. Therefore internet connection to "
                    + "https://pypi.python.org/simple/ must be available to run VTS tests.");
        }
    }

    /**
     * This method returns whether the given path is a dir that exists and the user has read access.
     */
    private boolean dirExistsAndHaveReadAccess(String path) {
        File pathDir = new File(path);
        if (!pathDir.exists() || !pathDir.isDirectory()) {
            CLog.d("Directory %s does not exist.", pathDir);
            return false;
        }

        if (!EnvUtil.isOnWindows()) {
            CommandResult c = getRunUtil().runTimedCmd(BASE_TIMEOUT, "ls", path);
            if (c.getStatus() != CommandStatus.SUCCESS) {
                CLog.d(String.format("Failed to read dir: %s. Result %s. stdout: %s, stderr: %s",
                        path, c.getStatus(), c.getStdout(), c.getStderr()));
                return false;
            }
            return true;
        } else {
            try {
                String[] pathDirList = pathDir.list();
                if (pathDirList == null) {
                    CLog.d("Failed to read dir: %s. Please check access permission.", pathDir);
                    return false;
                }
            } catch (SecurityException e) {
                CLog.d(String.format(
                        "Failed to read dir %s with SecurityException %s", pathDir, e));
                return false;
            }
            return true;
        }
    }

    protected void installDeps() throws TargetSetupError {
        boolean hasDependencies = false;
        if (!mScriptFiles.isEmpty()) {
            for (String scriptFile : mScriptFiles) {
                CLog.d("Attempting to execute a script, %s", scriptFile);
                CommandResult c = getRunUtil().runTimedCmd(BASE_TIMEOUT * 5, scriptFile);
                if (c.getStatus() != CommandStatus.SUCCESS) {
                    CLog.e("Executing script %s failed", scriptFile);
                    throw new TargetSetupError("Failed to source a script", mDescriptor);
                }
            }
        }
        if (mRequirementsFile != null) {
            CommandResult c = getRunUtil().runTimedCmd(BASE_TIMEOUT * 5, getPipPath(), "install",
                    "-r", mRequirementsFile.getAbsolutePath());
            if (!CommandStatus.SUCCESS.equals(c.getStatus())) {
                CLog.e("Installing dependencies from %s failed with error: %s",
                        mRequirementsFile.getAbsolutePath(), c.getStderr());
                throw new TargetSetupError("Failed to install dependencies with pip", mDescriptor);
            }
            hasDependencies = true;
        }
        if (!mDepModules.isEmpty()) {
            for (String dep : mDepModules) {
                if (mNoDepModules.contains(dep) || isPipModuleInstalled(dep)) {
                    continue;
                }
                CommandResult result = null;
                if (mLocalPypiPath != null) {
                    CLog.d("Attempting installation of %s from local directory", dep);
                    result = getRunUtil().runTimedCmd(BASE_TIMEOUT * 5, getPipPath(), "install",
                            dep, "--no-index", "--find-links=" + mLocalPypiPath);
                    CLog.d(String.format("Result %s. stdout: %s, stderr: %s", result.getStatus(),
                            result.getStdout(), result.getStderr()));
                    if (result.getStatus() != CommandStatus.SUCCESS) {
                        CLog.e(String.format("Installing %s from %s failed", dep, mLocalPypiPath));
                    }
                }
                if (mLocalPypiPath == null || result.getStatus() != CommandStatus.SUCCESS) {
                    CLog.d("Attempting installation of %s from PyPI", dep);
                    result = getRunUtil().runTimedCmd(
                            BASE_TIMEOUT * 5, getPipPath(), "install", dep);
                    CLog.d("Result %s. stdout: %s, stderr: %s", result.getStatus(),
                            result.getStdout(), result.getStderr());
                    if (result.getStatus() != CommandStatus.SUCCESS) {
                        CLog.e("Installing %s from PyPI failed.", dep);
                        CLog.d("Attempting to upgrade %s", dep);
                        result = getRunUtil().runTimedCmd(
                                BASE_TIMEOUT * 5, getPipPath(), "install", "--upgrade", dep);
                        if (result.getStatus() != CommandStatus.SUCCESS) {
                            throw new TargetSetupError(
                                    String.format("Failed to install dependencies with pip. "
                                                    + "Result %s. stdout: %s, stderr: %s",
                                            result.getStatus(), result.getStdout(),
                                            result.getStderr()),
                                    mDescriptor);
                        } else {
                            CLog.d(String.format("Result %s. stdout: %s, stderr: %s",
                                    result.getStatus(), result.getStdout(), result.getStderr()));
                        }
                    }
                }
                hasDependencies = true;
            }
        }
        if (!hasDependencies) {
            CLog.d("No dependencies to install");
        }
    }

    /**
     * This method returns absolute pip path in virtualenv.
     *
     * This method is needed because although PATH is set in IRunUtil, IRunUtil will still
     * use pip from system path.
     *
     * @return absolute pip path in virtualenv. null if virtualenv not available.
     */
    public String getPipPath() {
        if (mPipPath != null) {
            return mPipPath;
        }

        String virtualenvPath = mVenvDir.getAbsolutePath();
        if (virtualenvPath == null) {
            return null;
        }
        mPipPath = new File(VtsPythonRunnerHelper.getPythonBinDir(virtualenvPath), "pip")
                           .getAbsolutePath();
        return mPipPath;
    }

    /**
     * Get the major python version from option.
     *
     * Currently, only 2 and 3 are supported.
     *
     * @return major version number
     * @throws TargetSetupError
     */
    protected int getConfiguredPythonVersionMajor() throws TargetSetupError {
        if (mPythonVersion.startsWith("3.") || mPythonVersion.equals("3")) {
            return 3;
        } else if (mPythonVersion.startsWith("2.") || mPythonVersion.equals("2")) {
            return 2;
        } else {
            throw new TargetSetupError("Unsupported python version " + mPythonVersion);
        }
    }

    /**
     * Add PYTHONPATH and VIRTUAL_ENV_PATH to BuildInfo.
     * @param buildInfo
     * @throws TargetSetupError
     */
    protected void addPathToBuild(IBuildInfo buildInfo) throws TargetSetupError {
        String target = null;
        switch (getConfiguredPythonVersionMajor()) {
            case 2:
                target = VtsPythonVirtualenvPreparer.VIRTUAL_ENV;
                break;
            case 3:
                target = VtsPythonVirtualenvPreparer.VIRTUAL_ENV_V3;
                break;
        }

        if (!buildInfo.getBuildAttributes().containsKey(target)) {
            buildInfo.addBuildAttribute(target, mVenvDir.getAbsolutePath());
        }
    }

    /**
     * Create virtualenv directory by executing virtualenv command.
     * @param buildInfo
     * @throws TargetSetupError
     */
    protected void createVirtualenv(IBuildInfo buildInfo) throws TargetSetupError {
        if (mVenvDir == null) {
            String venvDir = null;
            switch (getConfiguredPythonVersionMajor()) {
                case 2:
                    venvDir = buildInfo.getBuildAttributes().get(
                            VtsPythonVirtualenvPreparer.VIRTUAL_ENV);
                    break;
                case 3:
                    venvDir = buildInfo.getBuildAttributes().get(
                            VtsPythonVirtualenvPreparer.VIRTUAL_ENV_V3);
                    break;
            }

            if (venvDir != null) {
                mVenvDir = new File(venvDir);
            }
        }

        if (mVenvDir != null) {
            return;
        }

        try {
            if (mReuse) {
                String tempDir = System.getProperty("java.io.tmpdir");
                mVenvDir = new File(tempDir, "vts-virtualenv-" + mPythonVersion);
                if (mVenvDir.exists()) {
                    CLog.d("Using existing virtualenv for version " + mPythonVersion);
                    // TODO(yuexima): handle multiple TF instance case race condition
                    return;
                }
            } else {
                mVenvDir = FileUtil.createTempDir("vts-virtualenv-" + mPythonVersion + "-"
                        + VtsFileUtil.normalizeFileName(buildInfo.getTestTag()) + "_");
            }
            CLog.d("Creating virtualenv for version " + mPythonVersion);
            mIsDirCreator = true;
            String virtualEnvPath = mVenvDir.getAbsolutePath();
            String[] cmd =
                    new String[] {"virtualenv", "-p", "python" + mPythonVersion, virtualEnvPath};
            CommandResult c = getRunUtil().runTimedCmd(BASE_TIMEOUT * 3, cmd);
            if (c.getStatus() != CommandStatus.SUCCESS) {
                CLog.e(String.format("Failed to create virtualenv with : %s.", virtualEnvPath));
                CLog.e(String.format("Exit code: %s, stdout: %s, stderr: %s", c.getStatus(),
                        c.getStdout(), c.getStderr()));
                throw new TargetSetupError("Failed to create virtualenv", mDescriptor);
            }
        } catch (IOException | RuntimeException e) {
            CLog.e("Failed to create temp directory for virtualenv");
            throw new TargetSetupError("Error creating virtualenv", e, mDescriptor);
        }
    }

    protected void addDepModule(String module) {
        mDepModules.add(module);
    }

    protected void setRequirementsFile(File f) {
        mRequirementsFile = f;
    }

    /**
     * Get an instance of {@link IRunUtil}.
     */
    @VisibleForTesting
    IRunUtil getRunUtil() {
        if (mRunUtil == null) {
            mRunUtil = new RunUtil();
        }
        return mRunUtil;
    }

    /**
     * This method recursively deletes a file tree without following symbolic links.
     *
     * @param rootPath the path to delete.
     * @throws IOException if fails to traverse or delete the files.
     */
    private static void recursiveDelete(Path rootPath) throws IOException {
        Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
                if (e != null) {
                    throw e;
                }
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Locally checks whether a pip module is installed.
     *
     * This read the installed module list from command "pip list" and check whether the
     * module in requirement string is installed and its version satisfied.
     *
     * Note: This method is only a help method for speed optimization purpose.
     *       It does not check dependencies of the module.
     *       It replace dots "." in module name with dash "-".
     *       If the "pip list" command failed, it will return false and will not throw exception
     *       It can also only accept one ">=" version requirement string.
     *       If this method returns false, the requirement should still be checked using pip itself.
     *
     * @param requirement such as "numpy", "pip>=9"
     * @return True if module is installed locally with correct version. False otherwise
     */
    private boolean isPipModuleInstalled(String requirement) {
        if (mPipInstallList == null) {
            mPipInstallList = getInstalledPipModules();
            if (mPipInstallList == null) {
                CLog.e("Failed to read local pip install list.");
                return false;
            }
        }

        String name;
        String version = null;
        if (requirement.contains(">=")) {
            String[] tokens = requirement.split(">=");
            if (tokens.length != 2) {
                return false;
            }
            name = tokens[0];
            version = tokens[1];
        } else if (requirement.contains("=") || requirement.contains("<")
                || requirement.contains(">")) {
            return false;
        } else {
            name = requirement;
        }

        name = name.replaceAll("\\.", "-");

        if (!mPipInstallList.containsKey(name)) {
            return false;
        }

        // TODO: support other comparison and multiple condition if there's a use case.
        if (version != null && !isVersionGreaterEqual(mPipInstallList.get(name), version)) {
            return false;
        }

        return true;
    }

    /**
     * Compares whether version string 1 is greater or equal to version string 2
     * @param version1
     * @param version2
     * @return True if the value of version1 >= version2
     */
    private static boolean isVersionGreaterEqual(String version1, String version2) {
        String[] tokens1 = version1.split("\\.");
        String[] tokens2 = version2.split("\\.");

        int length = Math.max(tokens1.length, tokens2.length);
        for (int i = 0; i < length; i++) {
            int token1 = i < tokens1.length ? Integer.parseInt(tokens1[i]) : 0;
            int token2 = i < tokens2.length ? Integer.parseInt(tokens2[i]) : 0;
            if (token1 < token2) {
                return false;
            }
        }

        return true;
    }

    /**
     * Gets map of installed pip packages and their versions.
     * @return installed pip packages
     */
    private Map<String, String> getInstalledPipModules() {
        CommandResult res = getRunUtil().runTimedCmd(BASE_TIMEOUT, getPipPath(), "list");
        if (res.getStatus() != CommandStatus.SUCCESS) {
            CLog.e(String.format("Failed to read pip installed list: "
                            + "Result %s. stdout: %s, stderr: %s",
                    res.getStatus(), res.getStdout(), res.getStderr()));
            return null;
        }
        String raw = res.getStdout();
        String[] lines = raw.split("\\r?\\n");

        TreeMap<String, String> pipInstallList = new TreeMap<>();

        for (String line : lines) {
            line = line.trim();
            if (line.length() == 0 || line.startsWith("Package ") || line.startsWith("-")) {
                continue;
            }
            String[] tokens = line.split("\\s+");
            if (tokens.length != 2) {
                CLog.e("Error parsing pip installed package list. Line text: " + line);
                continue;
            }
            pipInstallList.put(tokens[0], tokens[1]);
        }

        return pipInstallList;
    }
}
