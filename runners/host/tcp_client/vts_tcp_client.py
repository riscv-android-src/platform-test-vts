#!/usr/bin/env python
#
# Copyright (C) 2016 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import json
import logging
import os
import socket
import time
import types

from vts.proto import AndroidSystemControlMessage_pb2 as SysMsg_pb2
from vts.proto import ComponentSpecificationMessage_pb2 as CompSpecMsg_pb2
from vts.runners.host import const
from vts.runners.host import errors
from vts.utils.python.mirror import mirror_object

from google.protobuf import text_format

TARGET_IP = os.environ.get("TARGET_IP", None)
TARGET_PORT = os.environ.get("TARGET_PORT", None)
_DEFAULT_SOCKET_TIMEOUT_SECS = 120
_SOCKET_CONN_TIMEOUT_SECS = 60
_SOCKET_CONN_RETRY_NUMBER = 5
COMMAND_TYPE_NAME = {1: "LIST_HALS",
                     2: "SET_HOST_INFO",
                     101: "CHECK_DRIVER_SERVICE",
                     102: "LAUNCH_DRIVER_SERVICE",
                     103: "VTS_AGENT_COMMAND_READ_SPECIFICATION",
                     201: "LIST_APIS",
                     202: "CALL_API",
                     203: "VTS_AGENT_COMMAND_GET_ATTRIBUTE",
                     301: "VTS_AGENT_COMMAND_EXECUTE_SHELL_COMMAND"}


class VtsTcpClient(object):
    """VTS TCP Client class.

    Attribute:
        connection: a TCP socket instance.
        channel: a file to write and read data.
        _mode: the connection mode (adb_forwarding or ssh_tunnel)
    """

    def __init__(self, mode="adb_forwarding"):
        self.connection = None
        self.channel = None
        self._mode = mode

    def Connect(self, ip=TARGET_IP, command_port=TARGET_PORT,
                callback_port=None, retry=_SOCKET_CONN_RETRY_NUMBER):
        """Connects to a target device.

        Args:
            ip: string, the IP address of a target device.
            command_port: int, the TCP port which can be used to connect to
                          a target device.
            callback_port: int, the TCP port number of a host-side callback
                           server.
            retry: int, the number of times to retry connecting before giving
                   up.

        Returns:
            True if success, False otherwise

        Raises:
            socket.error when the connection fails.
        """
        if not command_port:
            logging.error("ip %s, command_port %s, callback_port %s invalid",
                          ip, command_port, callback_port)
            return False

        for i in xrange(retry):
            try:
                self.connection = socket.create_connection(
                    (ip, command_port), _SOCKET_CONN_TIMEOUT_SECS)
                self.connection.settimeout(_DEFAULT_SOCKET_TIMEOUT_SECS)
            except socket.error as e:
                # Wait a bit and retry.
                logging.exception("Connect failed %s", e)
                time.sleep(1)
                if i + 1 == retry:
                    raise errors.VtsTcpClientCreationError(
                        "Couldn't connect to %s:%s" % (ip, command_port))
        self.channel = self.connection.makefile(mode="brw")

        if callback_port is not None:
            self.SendCommand(SysMsg_pb2.SET_HOST_INFO,
                             callback_port=callback_port)
            resp = self.RecvResponse()
            if (resp.response_code != SysMsg_pb2.SUCCESS):
                return False
        return True

    def Disconnect(self):
        """Disconnects from the target device.

        TODO(yim): Send a msg to the target side to teardown handler session
        and release memory before closing the socket.
        """
        if self.connection is not None:
            self.channel = None
            self.connection.close()
            self.connection = None

    def ListHals(self, base_paths):
        """RPC to LIST_HALS."""
        self.SendCommand(SysMsg_pb2.LIST_HALS, paths=base_paths)
        resp = self.RecvResponse()
        if (resp.response_code == SysMsg_pb2.SUCCESS):
            return resp.file_names
        return None

    def CheckDriverService(self, service_name):
        """RPC to CHECK_DRIVER_SERVICE."""
        self.SendCommand(SysMsg_pb2.CHECK_DRIVER_SERVICE,
                         service_name=service_name)
        resp = self.RecvResponse()
        return (resp.response_code == SysMsg_pb2.SUCCESS)

    def LaunchDriverService(self, driver_type, service_name, bits,
                            file_path=None, target_class=None, target_type=None,
                            target_version=None, target_package=None):
        """RPC to LAUNCH_DRIVER_SERVICE."""
        logging.info("service_name: %s", service_name)
        logging.info("file_path: %s", file_path)
        logging.info("bits: %s", bits)
        logging.info("driver_type: %s", driver_type)
        self.SendCommand(SysMsg_pb2.LAUNCH_DRIVER_SERVICE,
                         driver_type=driver_type,
                         service_name=service_name,
                         bits=bits,
                         file_path=file_path,
                         target_class=target_class,
                         target_type=target_type,
                         target_version=target_version,
                         target_package=target_package)
        resp = self.RecvResponse()
        logging.info("resp for LAUNCH_DRIVER_SERVICE: %s", resp)
        return (resp.response_code == SysMsg_pb2.SUCCESS)

    def ListApis(self):
        """RPC to LIST_APIS."""
        self.SendCommand(SysMsg_pb2.LIST_APIS)
        resp = self.RecvResponse()
        logging.info("resp for LIST_APIS: %s", resp)
        if (resp.response_code == SysMsg_pb2.SUCCESS):
            return resp.spec
        return None

    def CallApi(self, arg):
        """RPC to CALL_API."""
        self.SendCommand(SysMsg_pb2.CALL_API, arg=arg)
        resp = self.RecvResponse()
        resp_code = resp.response_code
        if (resp_code == SysMsg_pb2.SUCCESS):
            result = CompSpecMsg_pb2.FunctionSpecificationMessage()
            if resp.result == "error":
                raise errors.VtsTcpCommunicationError(
                    "API call error by the VTS driver.")
            try:
                text_format.Merge(resp.result, result)
            except text_format.ParseError as e:
                logging.exception(e)
                logging.error("Paring error\n%s", resp.result)
            if result.return_type.type == CompSpecMsg_pb2.TYPE_SUBMODULE:
                logging.info("returned a submodule spec")
                logging.info("spec: %s", result.return_type_submodule_spec)
                return mirror_object.MirrorObject(
                     self, result.return_type_submodule_spec, None)
            if (len(result.return_type_hidl) == 1 and
                result.return_type_hidl[0].type ==
                    CompSpecMsg_pb2.TYPE_SCALAR):
                return getattr(result.return_type_hidl[0].scalar_value,
                               result.return_type_hidl[0].scalar_type)
            else:
                return result
        logging.error("NOTICE - Likely a crash discovery!")
        logging.error("SysMsg_pb2.SUCCESS is %s", SysMsg_pb2.SUCCESS)
        raise errors.VtsTcpCommunicationError(
            "RPC Error, response code for %s is %s" % (arg, resp_code))

    def GetAttribute(self, arg):
        """RPC to VTS_AGENT_COMMAND_GET_ATTRIBUTE."""
        self.SendCommand(SysMsg_pb2.VTS_AGENT_COMMAND_GET_ATTRIBUTE, arg=arg)
        resp = self.RecvResponse()
        resp_code = resp.response_code
        if (resp_code == SysMsg_pb2.SUCCESS):
            result = CompSpecMsg_pb2.FunctionSpecificationMessage()
            if resp.result == "error":
                raise errors.VtsTcpCommunicationError(
                    "Get attribute request failed on target.")
            try:
                text_format.Merge(resp.result, result)
            except text_format.ParseError as e:
                logging.exception(e)
                logging.error("Paring error\n%s", resp.result)
            if result.return_type.type == CompSpecMsg_pb2.TYPE_SUBMODULE:
                logging.info("returned a submodule spec")
                logging.info("spec: %s", result.return_type_submodule_spec)
                return mirror_object.MirrorObject(self,
                                           result.return_type_submodule_spec,
                                           None)
            elif result.return_type.type == CompSpecMsg_pb2.TYPE_SCALAR:
                return getattr(result.return_type.scalar_value,
                               result.return_type.scalar_type)
            return result
        logging.error("NOTICE - Likely a crash discovery!")
        logging.error("SysMsg_pb2.SUCCESS is %s", SysMsg_pb2.SUCCESS)
        raise errors.VtsTcpCommunicationError(
            "RPC Error, response code for %s is %s" % (arg, resp_code))

    def ExecuteShellCommand(self, command):
        """RPC to VTS_AGENT_COMMAND_EXECUTE_SHELL_COMMAND."""
        self.SendCommand(
            SysMsg_pb2.VTS_AGENT_COMMAND_EXECUTE_SHELL_COMMAND,
            shell_command=command)
        resp = self.RecvResponse(retries=2)
        logging.info("resp for VTS_AGENT_COMMAND_EXECUTE_SHELL_COMMAND: %s",
                     resp)
        if resp is not None and resp.response_code == SysMsg_pb2.SUCCESS:
            return {const.STDOUT: resp.stdout,
                    const.STDERR: resp.stderr,
                    const.EXIT_CODE: resp.exit_code,
                    }
        return {}

    def Ping(self):
        """RPC to send a PING request.

        Returns:
            True if the agent is alive, False otherwise.
        """
        self.SendCommand(SysMsg_pb2.PING)
        resp = self.RecvResponse()
        logging.info("resp for PING: %s", resp)
        if resp is not None and resp.response_code == SysMsg_pb2.SUCCESS:
            return True
        return False

    def ReadSpecification(self, interface_name):
        """RPC to VTS_AGENT_COMMAND_READ_SPECIFICATION."""
        self.SendCommand(
            SysMsg_pb2.VTS_AGENT_COMMAND_READ_SPECIFICATION,
            service_name=interface_name)
        resp = self.RecvResponse(retries=2)
        logging.info("resp for VTS_AGENT_COMMAND_EXECUTE_READ_INTERFACE: %s",
                     resp)
        logging.info("proto: %s",
                     resp.result)
        result = CompSpecMsg_pb2.ComponentSpecificationMessage()
        if resp.result == "error":
            raise errors.VtsTcpCommunicationError(
                "API call error by the VTS driver.")
        try:
            text_format.Merge(resp.result, result)
        except text_format.ParseError as e:
            logging.exception(e)
            logging.error("Paring error\n%s", resp.result)
        return result

    def SendCommand(self,
                    command_type,
                    paths=None,
                    file_path=None,
                    bits=None,
                    target_class=None,
                    target_type=None,
                    target_version=None,
                    target_package=None,
                    module_name=None,
                    service_name=None,
                    callback_port=None,
                    driver_type=None,
                    shell_command=None,
                    arg=None):
        """Sends a command.

        Args:
            command_type: integer, the command type.
            each of the other args are to fill in a field in
            AndroidSystemControlCommandMessage.
        """
        if not self.channel:
            raise errors.VtsTcpCommunicationError(
                "channel is None, unable to send command.")

        command_msg = SysMsg_pb2.AndroidSystemControlCommandMessage()
        command_msg.command_type = command_type
        logging.info("sending a command (type %s)",
                     COMMAND_TYPE_NAME[command_type])
        if command_type == 202:
            logging.info("target API: %s", arg)

        if target_class is not None:
            command_msg.target_class = target_class

        if target_type is not None:
            command_msg.target_type = target_type

        if target_version is not None:
            command_msg.target_version = int(target_version * 100)

        if target_package is not None:
            command_msg.target_package = target_package

        if module_name is not None:
            command_msg.module_name = module_name

        if service_name is not None:
            command_msg.service_name = service_name

        if driver_type is not None:
            command_msg.driver_type = driver_type

        if paths is not None:
            command_msg.paths.extend(paths)

        if file_path is not None:
            command_msg.file_path = file_path

        if bits is not None:
            command_msg.bits = bits

        if callback_port is not None:
            command_msg.callback_port = callback_port

        if arg is not None:
            command_msg.arg = arg

        if shell_command is not None:
            if isinstance(shell_command, types.ListType):
                command_msg.shell_command.extend(shell_command)
            else:
                command_msg.shell_command.append(shell_command)

        logging.info("command %s" % command_msg)
        message = command_msg.SerializeToString()
        message_len = len(message)
        logging.debug("sending %d bytes", message_len)
        self.channel.write(str(message_len) + b'\n')
        self.channel.write(message)
        self.channel.flush()

    def RecvResponse(self, retries=0):
        """Receives and parses the response, and returns the relevant ResponseMessage.

        Args:
            retries: an integer indicating the max number of retries in case of
                     session timeout error.
        """
        for index in xrange(1 + retries):
            try:
                if index != 0:
                    logging.info("retrying...")
                header = self.channel.readline()
                len = int(header.strip("\n"))
                logging.info("resp %d bytes", len)
                data = self.channel.read(len)
                response_msg = SysMsg_pb2.AndroidSystemControlResponseMessage()
                response_msg.ParseFromString(data)
                logging.debug("Response %s", "success"
                              if response_msg.response_code == SysMsg_pb2.SUCCESS
                              else "fail")
                return response_msg
            except socket.timeout as e:
                logging.exception(e)
        return None
