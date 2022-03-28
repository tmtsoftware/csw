/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package example.framework;

import csw.framework.javadsl.JContainerCmd;
import csw.prefix.javadsl.JSubsystem;

import java.util.Optional;

//#container-app
public class JContainerCmdApp {

    public static void main(String[] args) {
        JContainerCmd.start("JContainer-Cmd-App", JSubsystem.CSW,args, Optional.empty());
    }

}
//#container-app
