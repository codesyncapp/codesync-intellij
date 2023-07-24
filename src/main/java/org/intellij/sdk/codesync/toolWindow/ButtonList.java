package org.intellij.sdk.codesync.toolWindow;

import com.intellij.openapi.project.Project;
import org.intellij.sdk.codesync.state.PluginState;
import org.intellij.sdk.codesync.state.StateUtils;
import org.intellij.sdk.codesync.utils.CommonUtils;
import org.intellij.sdk.codesync.utils.ProjectUtils;

import javax.swing.*;
import java.awt.*;

public class ButtonList extends JPanel {

    static boolean userLoggedIn = false;
    static boolean repoConnected = false;
    static boolean needToAvailTrail = false;
    static boolean needToUpgradeToPro = false;
    static boolean isNestedRepo = false;
    static boolean isIgnoredNestedRepo = true;

    CodeSyncLabel loginMessage = new CodeSyncLabel("Login and connect the repository to start streaming your code.");
    CodeSyncLabel connectRepoMessage = new CodeSyncLabel("Great! Now just connect the repository to start streaming your code.");
    CodeSyncLabel userCanStreamMessage = new CodeSyncLabel("Your repository is in sync.");
    CodeSyncLabel tryProForFreeMessage = new CodeSyncLabel("Your repository size is exceeding the free limit. \n Try Pro for free to continue using CodeSync.");
    CodeSyncLabel upgradeToProMessage = new CodeSyncLabel("Your free trial of CodeSync has ended. \n Please upgrade your plan to continue using CodeSync, or contact sales.");
    CodeSyncLabel nestedDirMessage = new CodeSyncLabel("This file is ignored by .syncignore and is not in sync with CodeSync.");
    CodeSyncLabel nestedIgnoredDirMessage = new CodeSyncLabel("Current directory is ignored by a parent directory for streaming. \n To include this directory/file, remove it from the .syncignore file.");


    CodeSyncButton login = new CodeSyncButton("Login");
    CodeSyncButton logout = new CodeSyncButton("Logout");
    CodeSyncButton connectRepository = new CodeSyncButton("Connect Repository");
    CodeSyncButton disconnectRepository = new CodeSyncButton("Disconnect Repository");
    CodeSyncButton viewFilePlayback = new CodeSyncButton("View File Playback");
    CodeSyncButton viewDashboard = new CodeSyncButton("View Dashboard");
    CodeSyncButton tryProForFree = new CodeSyncButton("Try Pro For Free");
    CodeSyncButton upgradeToPro = new CodeSyncButton("Upgrade To Pro");
    CodeSyncButton openSyncIgnoreFile = new CodeSyncButton("Open .syncignore File");
    CodeSyncButton viewParentRepositoryOnWeb = new CodeSyncButton("View Parent Repository on Web");
    CodeSyncButton unsyncParentRepository = new CodeSyncButton("Unsync Parent Repository");

    public ButtonList(){
        this.addingActionListeners();
        paint();
    }

    private void paint(){
        this.removeAll();
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        Project project = CommonUtils.getCurrentProject();
        String repoPath = ProjectUtils.getRepoPath(project);
        PluginState pluginState = StateUtils.getState(repoPath);
        PluginState globalState = StateUtils.getGlobalState();

        if(!userLoggedIn){   //Replace it with !globalState.isAuthenticated
            userNotLoggedIn();
        }else if(isNestedRepo){
            if(isIgnoredNestedRepo){
                syncIgnoredNestedDirOpen();
            }else{
                nestedDirOpen();
            }
        }else if(!repoConnected){ //Replace it with !pluginState.isRepoInSync
            repositoryNotConnected();
        }else if(needToAvailTrail){
            userNeedToAvailTrail();
        }else if(needToUpgradeToPro){
            userNeedToUpgradeToPro();
        }else {
            userCanStream();
        }

        this.setAlignmentX(Component.CENTER_ALIGNMENT);
    }

    private void addingActionListeners(){
        login.addActionListener(e -> loginAction());
        logout.addActionListener(e -> logoutAction());
        connectRepository.addActionListener(e -> connectRepositoryAction());
        disconnectRepository.addActionListener(e -> disconnectRepositoryAction());
        viewFilePlayback.addActionListener(e -> viewFilePlaybackAction());
        viewDashboard.addActionListener(e -> viewDashboardAction());
        tryProForFree.addActionListener(e -> tryProForFreeAction());
        upgradeToPro.addActionListener(e -> upgradeToProAction());
        openSyncIgnoreFile.addActionListener(e -> openSyncIgnoreFileAction());
        viewParentRepositoryOnWeb.addActionListener(e -> viewParentRepositoryOnWebAction());
        unsyncParentRepository.addActionListener(e -> unsyncParentRepositoryAction());
    }

    private void loginAction(){
        System.out.println("Login Button Clicked");
        this.userLoggedIn = true;
        CodeSyncToolWindow.updateMenu();
    }

    private void logoutAction(){
        System.out.println("Logout Button Clicked");
        this.userLoggedIn = false;
        CodeSyncToolWindow.updateMenu();
    }

    private void connectRepositoryAction(){
        System.out.println("Connect Repository Button Clicked");
        this.repoConnected = true;
        CodeSyncToolWindow.updateMenu();
    }

    private void disconnectRepositoryAction(){
        System.out.println("Disconnect Repository Button Clicked");
        this.repoConnected = false;
        CodeSyncToolWindow.updateMenu();
    }

    private void viewFilePlaybackAction(){
        System.out.println("View File Playback Button Clicked");
    }

    private void viewDashboardAction(){
        System.out.println("View Dashboard Button Clicked");
    }

    private void tryProForFreeAction(){
        System.out.println("Try Pro For Free Button Clicked");
    }

    private void upgradeToProAction(){
        System.out.println("Upgrade To Pro Button Clicked");
    }

    private void openSyncIgnoreFileAction(){
        System.out.println("Open SyncIgnore File Button Clicked");
    }

    private void viewParentRepositoryOnWebAction(){
        System.out.println("View Parent Repository On Web Button Clicked");
    }

    private void unsyncParentRepositoryAction(){
        System.out.println("Unsync Parent Repository Button Clicked");
    }

    private void userNotLoggedIn(){
        this.add(loginMessage);
        this.add(login);
    }

    private void repositoryNotConnected(){
        this.add(connectRepoMessage);
        this.add(connectRepository);
        this.add(logout);
    }

    private void userCanStream(){
        this.add(userCanStreamMessage);
        this.add(viewFilePlayback);
        this.add(viewDashboard);
        this.add(disconnectRepository);
        this.add(logout);
    }

    private void userNeedToAvailTrail(){
        this.add(tryProForFreeMessage);
        this.add(viewFilePlayback);
        this.add(viewDashboard);
        this.add(tryProForFree);
        this.add(disconnectRepository);
        this.add(logout);
    }

    private void userNeedToUpgradeToPro(){
        this.add(upgradeToProMessage);
        this.add(viewFilePlayback);
        this.add(viewDashboard);
        this.add(upgradeToPro);
        this.add(disconnectRepository);
        this.add(logout);
    }

    private void syncIgnoredNestedDirOpen(){
        this.add(nestedIgnoredDirMessage);
        this.add(openSyncIgnoreFile);
        this.add(viewParentRepositoryOnWeb);
        this.add(unsyncParentRepository);
    }

    private void nestedDirOpen(){
        this.add(nestedDirMessage);
        this.add(viewParentRepositoryOnWeb);
        this.add(unsyncParentRepository);
    }

}
