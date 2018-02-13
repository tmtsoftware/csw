package csw.framework;

import csw.framework.javadsl.JContainerCmd;

import java.util.Optional;

//#container-app
public class JContainerCmdApp {

    public static void main(String args[]) {
        JContainerCmd.start("JContainer-Cmd-App", args, Optional.empty());
    }

}
//#container-app
