package jgittest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

public class JGitTest
{
    public static final boolean DO = true;
    public static final boolean USE_CUSTOM_CREDENTIALS_PROVIDER = true;

    public static final String LS = System.getProperty("line.separator");

    private static File DEFAULT_REPOISTORY = new File(
        "C:/AndroidStudioProjects/BLE Cardiac Monitor");
    private static String DEFAULT_REMOTE = "git@git.mfgames.com:KennethEvans/Bluetooth-Cardiac-Monitor.git";

    /** The default repository locations. */
    private static File[] REPOSITORY_LOCATIONS_DEFAULT = {
        // Start
        new File("C:/Git/SVN/AppInfo"),
        new File("C:/Git/jgit-cookbook/jgit-cookbook"),
        new File("C:/AndroidStudioProjects/BLE Cardiac Monitor"),
        new File("C:/AndroidStudioProjects/Heart Notes"),
        new File("C:/AndroidStudioProjects/Map Image"),
        // End
    };
    /** Whether to use the default repository locations or not. */
    private static final boolean USE_DEFAULT_REPOSITORY_LOCATIONS = false;
    /** The repository locations actually used. */
    private static File[] repositoryLocations = REPOSITORY_LOCATIONS_DEFAULT;
    /** The default directories for repository locations. */
    private static final File[] PROJECT_DIRS = {
        // Start
        new File("C:/AndroidStudioProjects"),
        // new File("C:/Git/SVN"),
        // End
    };

    protected static void push() {
        System.out.println("*** Push");
        boolean all = true;
        boolean force = true;
        boolean dryRun = true;
        String remote = Constants.DEFAULT_REMOTE_NAME; // "origin"
        // String receivePack = RemoteConfig.DEFAULT_RECEIVE_PACK;
        // boolean thin = Transport.DEFAULT_PUSH_THIN;
        // boolean tags = true;
        // int timeout = 30;
        // List<RefSpec> refSpecs = new ArrayList<RefSpec>();
        //
        // Repository repository;

        Git git;
        File file = DEFAULT_REPOISTORY;
        System.out.println(file.getPath());
        CredentialsProvider cp = null;
        try {
            try {
                git = Git.open(file);
            } catch(RepositoryNotFoundException ex) {
                System.out.println("Repository not found");
                return;
            }
            // repository = git.getRepository();
            
            // Get the status of the repository
            String statusInfo = getRepositoryStatus(git);
            if(statusInfo != null) {
                System.out.println();
                System.out.println("Status");
                System.out.println(statusInfo);
            }

            // Prompt for credentials
            String password = null;
            String name = null;
            JLabel userNameLabel = new JLabel("User Name");
            JTextField userNameTF = new JTextField();
            JLabel passwordLabel = new JLabel("Password");
            JTextField passwordField = new JPasswordField();
            // String[] item = new String[]{"Male", "Female"};
            // JComboBox box = new JComboBox(item);
            Object[] msgItems = {userNameLabel, userNameTF, passwordLabel,
                passwordField};
            int res = JOptionPane.showConfirmDialog(null, msgItems,
                "Enter Credentials", JOptionPane.OK_CANCEL_OPTION);
            if(res == JOptionPane.OK_OPTION) {
                name = userNameTF.getText();
                userNameTF.setText(null);
                password = passwordField.getText();
                passwordField.setText(null);
            }
            if(name == null || password == null) {
                System.out.println("Aborting");
                return;
            }

            // Make a credentials provider
            if(USE_CUSTOM_CREDENTIALS_PROVIDER) {
                cp = new CustomCredentialsProvider(name, password);
            } else {
                cp = new UsernamePasswordCredentialsProvider(name, password);
            }
            boolean ok;
            name = null;
            password = null;

            // DEBUG
            CredentialItem ciPassword = new CredentialItem.Password();
            ok = cp.supports(ciPassword);
            System.out.println("Supports Password: " + ok);
            CredentialItem ciUsername = new CredentialItem.Username();
            ok = cp.supports(ciUsername);
            System.out.println("Supports Username: " + ok);
            CredentialItem ciStringType = new CredentialItem.StringType(
                "Enter Password", true);
            ok = cp.supports(ciStringType);
            System.out.println("Supports StringType: " + ok);

            // Push
            PushCommand pushCmd = git.push();
            pushCmd.setCredentialsProvider(cp).setRemote(remote).setForce(force)
                .setDryRun(dryRun);
            if(all) {
                pushCmd.setPushAll();
            }

            System.out.println("Processing...");
            Iterable<PushResult> results = pushCmd.call();
            if(results != null) {
                String info;
                for(PushResult result : results) {
                    info = handlePushResult(result);
                    if(info != null) {
                        System.out.println(info);
                    }
                }
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Creates a string message to be displayed on client side to inform the
     * user about the push operation.
     */
    public static String handlePushResult(PushResult pushResult) {
        StringBuffer sb = new StringBuffer();
        sb.append(pushResult.getMessages());
        sb.append(LS);
        for(RemoteRefUpdate rru : pushResult.getRemoteUpdates()) {
            String remoteName = rru.getRemoteName();
            RemoteRefUpdate.Status status = rru.getStatus();
            sb.append(remoteName);
            sb.append(" -> ");
            sb.append(status.name());
            sb.append(LS);
        }
        return sb.toString();
    }

    /**
     * Get status info.
     * 
     * @param git The Git to use.
     * @return
     */
    public static String getRepositoryStatus(Git git) {
        StringBuffer sb = new StringBuffer();
        Status status = null;
        try {
            status = git.status().call();
        } catch(NoWorkTreeException ex) {
            sb.append(ex.getMessage() + LS);
        } catch(GitAPIException ex) {
            sb.append(ex.getMessage() + LS);
        }

        boolean isClean = status.isClean();
        sb.append("Clean: " + status.isClean() + LS);
        if(!isClean) {
            sb.append("Added: " + status.getAdded() + LS);
            sb.append("Changed: " + status.getChanged() + LS);
            sb.append("Conflicting: " + status.getConflicting() + LS);
            sb.append("ConflictingStageState: "
                + status.getConflictingStageState() + LS);
            sb.append(
                "IgnoredNotInIndex: " + status.getIgnoredNotInIndex() + LS);
            sb.append("Missing: " + status.getMissing() + LS);
            sb.append("Modified: " + status.getModified() + LS);
            sb.append("Removed: " + status.getRemoved() + LS);
            sb.append("Untracked: " + status.getUntracked() + LS);
            sb.append("UntrackedFolders: " + status.getUntrackedFolders() + LS);
        }

        // Branch tracking
        sb.append(LS);
        Repository repository = git.getRepository();
        List<Ref> call;
        try {
            call = git.branchList().call();
            for(Ref ref : call) {
                List<Integer> counts;
                try {
                    counts = getTrackingCounts(repository, ref.getName(), true);
                    sb.append("For branch: " + ref.getName() + LS);
                    sb.append("Commits ahead : " + counts.get(0) + LS);
                    sb.append("Commits behind : " + counts.get(1) + LS);
                } catch(IOException ex) {
                    // TODO Auto-generated catch block
                    sb.append("Error getting counts: " + ex.getMessage());
                    continue;
                }
            }
        } catch(GitAPIException ex1) {
            sb.append("Error getting branch list: " + ex1.getMessage());
        }

        return sb.toString();
    }

    /**
     * Attempted push from the web.
     * {@link https://github.com/imyousuf/jgit/blob/master/org.eclipse.jgit.pgm/src/org/eclipse/jgit/pgm/Push.java}
     */
    protected static void pushBad() {
        System.out.println("*** Push");
        boolean thin = Transport.DEFAULT_PUSH_THIN;
        boolean all = true;
        boolean tags = true;
        boolean force = true;
        boolean dryRun = true;
        String receivePack = RemoteConfig.DEFAULT_RECEIVE_PACK;
        int timeout = 30;
        List<RefSpec> refSpecs = new ArrayList<RefSpec>();

        Repository repository;
        Git git;
        File file = DEFAULT_REPOISTORY;
        System.out.println(file.getPath());
        try {
            try {
                git = Git.open(file);
            } catch(RepositoryNotFoundException ex) {
                System.out.println("Repository not found");
                return;
            }
            repository = git.getRepository();

            PushCommand push = git.push();
            push.setDryRun(dryRun);
            push.setForce(force);
            push.setProgressMonitor(new TextProgressMonitor());
            push.setReceivePack(receivePack);
            push.setRefSpecs(refSpecs);
            if(all) push.setPushAll();
            if(tags) push.setPushTags();
            push.setRemote(DEFAULT_REMOTE);
            push.setThin(thin);
            push.setTimeout(timeout);
            Iterable<PushResult> results = push.call();
            ObjectReader reader;
            for(PushResult result : results) {
                reader = repository.newObjectReader();
                try {
                    System.out.println(result.getURI());
                    // printPushResult(reader, result.getURI(), result);
                } finally {
                    // reader.release();
                    if(reader != null) reader.close();
                }
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Gets the status and branch tracking of the repository locations.
     * 
     * @param doStatus Show the status or not.
     * @param doBranchTracking Show the tracking or not.
     * @param full For the status show the full status even if clean.
     */
    private static void getStatus(boolean doStatus, boolean doBranchTracking,
        boolean full, boolean summaryOnly) {
        System.out.println("*** Status");
        Repository repository;
        Status status;
        Git git;
        boolean isClean;
        int dirtyCount = 0;
        int trackingCountAhead = 0;
        int trackingCountBehind = 0;
        int noTrackingCount = 0;
        int notFoundCount = 0;
        ArrayList<File> dirtyFiles = new ArrayList<>();
        ArrayList<File> aheadFiles = new ArrayList<>();
        ArrayList<File> behindFiles = new ArrayList<>();
        ArrayList<File> noTrackingFiles = new ArrayList<>();
        ArrayList<File> notFoundFiles = new ArrayList<>();
        String tab = "    ";
        for(File file : repositoryLocations) {
            if(!summaryOnly) {
                System.out.println(file.getPath());
            }
            try {
                try {
                    git = Git.open(file);
                } catch(RepositoryNotFoundException ex) {
                    if(!summaryOnly) {
                        System.out.println("Repository not found");
                    }
                    notFoundCount++;
                    notFoundFiles.add(file);
                    continue;
                }

                // Status
                if(doStatus) {
                    status = git.status().call();
                    isClean = status.isClean();
                    if(!isClean) {
                        dirtyCount++;
                        dirtyFiles.add(file);
                    }
                    if(!summaryOnly) {
                        System.out.println("Clean: " + status.isClean());
                        if(full || !isClean) {
                            System.out.println("Added: " + status.getAdded());
                            System.out
                                .println("Changed: " + status.getChanged());
                            System.out.println(
                                "Conflicting: " + status.getConflicting());
                            System.out.println("ConflictingStageState: "
                                + status.getConflictingStageState());
                            System.out.println("IgnoredNotInIndex: "
                                + status.getIgnoredNotInIndex());
                            System.out
                                .println("Missing: " + status.getMissing());
                            System.out
                                .println("Modified: " + status.getModified());
                            System.out
                                .println("Removed: " + status.getRemoved());
                            System.out
                                .println("Untracked: " + status.getUntracked());
                            System.out.println("UntrackedFolders: "
                                + status.getUntrackedFolders());
                        }
                    }
                }

                // Branch tracking
                if(doBranchTracking) {
                    repository = git.getRepository();
                    List<Ref> call = git.branchList().call();
                    for(Ref ref : call) {
                        List<Integer> counts = getTrackingCounts(repository,
                            ref.getName(), !summaryOnly);
                        if(counts.get(0) > 0) {
                            trackingCountAhead++;
                            aheadFiles.add(file);
                        }
                        if(counts.get(1) > 0) {
                            trackingCountBehind++;
                            behindFiles.add(file);
                        }
                        if(counts.get(0) < 0 || counts.get(1) < 0) {
                            noTrackingCount++;
                            noTrackingFiles.add(file);
                        }
                        if(!summaryOnly) {

                            System.out.println("For branch: " + ref.getName());
                            System.out
                                .println("Commits ahead : " + counts.get(0));
                            System.out
                                .println("Commits behind : " + counts.get(1));
                        }
                    }
                }
                if(!summaryOnly) {
                    System.out.println();
                }
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }

        // Summary
        System.out.println();
        System.out
            .println("Dirty: " + dirtyCount + ", Behind: " + trackingCountBehind
                + ", Ahead: " + trackingCountAhead + ", No tracking: "
                + noTrackingCount + ", Not found: " + notFoundCount);
        if(dirtyCount > 0) {
            System.out.println();
            System.out.println("Dirty");
            for(File file : dirtyFiles) {
                System.out.println(tab + file.getPath());
            }
        }
        if(trackingCountBehind > 0) {
            System.out.println();
            System.out.println("Behind");
            for(File file : behindFiles) {
                System.out.println(tab + file.getPath());
            }
        }
        if(trackingCountAhead > 0) {
            System.out.println();
            System.out.println("Ahead");
            for(File file : aheadFiles) {
                System.out.println(tab + file.getPath());
            }
        }
        if(noTrackingCount > 0) {
            System.out.println();
            System.out.println("No Tracking");
            for(File file : noTrackingFiles) {
                System.out.println(tab + file.getPath());
            }
        }
        if(notFoundCount > 0) {
            System.out.println();
            System.out.println("Not Found");
            for(File file : notFoundFiles) {
                System.out.println(tab + file.getPath());
            }
        }
    }

    private static void getTrackingInfo() {
        System.out.println("*** Tracking Info");
        Repository repository;
        Git git;
        for(File file : repositoryLocations) {
            if(!file.isDirectory()) {
                continue;
            }
            System.out.println(file.getPath());
            try {
                try {
                    git = Git.open(file);
                } catch(RepositoryNotFoundException ex) {
                    System.out.println("Repository not found");
                    continue;
                }
                repository = git.getRepository();
                List<Ref> call = git.branchList().call();
                for(Ref ref : call) {
                    List<Integer> counts = getTrackingCounts(repository,
                        ref.getName(), true);
                    System.out.println("For branch: " + ref.getName());
                    System.out.println("Commits ahead : " + counts.get(0));
                    System.out.println("Commits behind : " + counts.get(1));
                }
                System.out.println();
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * A test method.
     */
    private static void test() {
        System.out.println("*** Status");
        // FileRepositoryBuilder repositoryBuilder;
        // Repository repository;
        Git git;
        Status status;
        for(File file : repositoryLocations) {
            System.out.println(file.getPath());
            try {
                // repositoryBuilder = new FileRepositoryBuilder();
                // repositoryBuilder.setMustExist(true);
                // repositoryBuilder.setWorkTree(file);
                // repository = repositoryBuilder.build();
                // git = new Git(repository);

                try {
                    git = Git.open(file);
                } catch(RepositoryNotFoundException ex) {
                    System.out.println("Repository not found");
                    continue;
                }
                status = git.status().call();
                if(DO) {
                    // Ref head = git.getRepository().findRef("HEAD");
                    // Ref head =
                    // git.getRepository().findRef("refs/heads/master");
                    // System.out.println(
                    // "Ref of HEAD: " + head + ": " + head.getName() + " - " +
                    // head.getObjectId().getName());

                    System.out.println("Clean: " + status.isClean());
                    System.out.println("Added: " + status.getAdded());
                    System.out.println("Changed: " + status.getChanged());
                    System.out
                        .println("Conflicting: " + status.getConflicting());
                    System.out.println("ConflictingStageState: "
                        + status.getConflictingStageState());
                    System.out.println(
                        "IgnoredNotInIndex: " + status.getIgnoredNotInIndex());
                    System.out.println("Missing: " + status.getMissing());
                    System.out.println("Modified: " + status.getModified());
                    System.out.println("Removed: " + status.getRemoved());
                    System.out.println("Untracked: " + status.getUntracked());
                    System.out.println(
                        "UntrackedFolders: " + status.getUntrackedFolders());
                    System.out.println();
                } else {

                }
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * List the remotes for the repository locations.
     */
    private static void printRemotes() {
        System.out.println("*** Print Remotes");
        Repository repository;
        Git git;
        for(File file : repositoryLocations) {
            System.out.println(file.getPath());
            try {
                try {
                    git = Git.open(file);
                } catch(RepositoryNotFoundException ex) {
                    System.out.println("Repository not found");
                    continue;
                }
                repository = git.getRepository();
                Config storedConfig = repository.getConfig();
                Set<String> remotes = storedConfig.getSubsections("remote");
                if(remotes.size() == 0) {
                    System.out.println("none");
                } else {
                    for(String remoteName : remotes) {
                        String url = storedConfig.getString("remote",
                            remoteName, "url");
                        System.out.println(remoteName + " " + url);
                    }
                }
            } catch(Exception ex) {
                ex.printStackTrace();
            }
            System.out.println();
        }
    }

    /**
     * List the branches for the repository locations.
     */
    private static void listBranches() {
        System.out.println("*** List Branches");
        Git git;
        for(File file : repositoryLocations) {
            System.out.println(file.getPath());
            try {
                try {
                    git = Git.open(file);
                } catch(RepositoryNotFoundException ex) {
                    System.out.println("Repository not found");
                    continue;
                }
                List<Ref> call = git.branchList().call();
                for(Ref ref : call) {
                    System.out.println("Branch: " + ref + " " + ref.getName()
                        + " " + ref.getObjectId().getName());
                }

                System.out.println("Now including remote branches:");
                call = git.branchList().setListMode(ListMode.ALL).call();
                for(Ref ref : call) {
                    System.out.println("Branch: " + ref + " " + ref.getName()
                        + " " + ref.getObjectId().getName());
                }
                System.out.println();
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * List the tracking status (commits ahead and behind) for the repository
     * locations.
     */
    private static void showBranchTrackingStatus() {
        System.out.println("*** Branch Tracking Status");
        Repository repository;
        Git git;
        for(File file : repositoryLocations) {
            System.out.println(file.getPath());
            try {
                try {
                    git = Git.open(file);
                } catch(RepositoryNotFoundException ex) {
                    System.out.println("Repository not found");
                    continue;
                }
                repository = git.getRepository();
                List<Ref> call = git.branchList().call();
                for(Ref ref : call) {
                    List<Integer> counts = getTrackingCounts(repository,
                        ref.getName(), true);
                    System.out.println("For branch: " + ref.getName());
                    System.out.println("Commits ahead : " + counts.get(0));
                    System.out.println("Commits behind : " + counts.get(1));
                }
                System.out.println();
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * List the counts for tracking information.
     * 
     * @param repository
     * @param branchName
     * @return
     * @throws IOException
     */
    private static List<Integer> getTrackingCounts(
        org.eclipse.jgit.lib.Repository repository, String branchName,
        boolean doPrint) throws IOException {
        BranchTrackingStatus trackingStatus = BranchTrackingStatus
            .of(repository, branchName);
        List<Integer> counts = new ArrayList<>();
        if(trackingStatus != null) {
            // System.out.println("Remote tracking branch " +
            // trackingStatus.getRemoteTrackingBranch());
            counts.add(trackingStatus.getAheadCount());
            counts.add(trackingStatus.getBehindCount());
        } else {
            if(doPrint) {
                System.out
                    .println("There is likely no remote tracking of branch "
                        + branchName);
            }
            counts.add(-1);
            counts.add(-1);
        }
        return counts;
    }

    /**
     * List the results of lsRemote for the repositories for the repository
     * locations.
     */
    private static void listRepositories() {
        System.out.println("*** List Repositories");
        Git git;
        for(File file : repositoryLocations) {
            System.out.println(file.getPath());
            try {
                try {
                    git = Git.open(file);
                } catch(RepositoryNotFoundException ex) {
                    System.out.println("Repository not found");
                    continue;
                }
                Collection<Ref> refs = git.lsRemote().call();
                for(Ref ref : refs) {
                    System.out.println("Ref: " + ref);
                }

                // heads only
                refs = git.lsRemote().setHeads(true).call();
                for(Ref ref : refs) {
                    System.out.println("Head: " + ref);
                }

                // tags only
                refs = git.lsRemote().setTags(true).call();
                for(Ref ref : refs) {
                    System.out.println("Remote tag: " + ref);
                }
                System.out.println();
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Sets the repository locations for the given parent directories.
     * 
     * @param parents The parent directory that have repository locations in
     *            them.
     * @return
     */
    public static File[] setRepositoryLocationsFromDir(File[] parents) {
        ArrayList<File> newFiles = new ArrayList<>();
        for(File dir : parents) {
            File[] files = dir.listFiles();
            for(File file : files) {
                if(file.isDirectory()) {
                    newFiles.add(file);
                }
            }
        }
        File[] repositoryFiles = new File[newFiles.size()];
        return newFiles.toArray(repositoryFiles);
    }

    /**
     * Main method.
     * 
     * @param args
     */
    public static void main(String[] args) {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.WARN);
        if(!USE_DEFAULT_REPOSITORY_LOCATIONS) {
            repositoryLocations = setRepositoryLocationsFromDir(PROJECT_DIRS);
        }

        if(DO) {
            push();
        } else {
            getStatus(true, true, false, true);
            test();
            getTrackingInfo();
            showBranchTrackingStatus();
            test();
            printRemotes();
            listBranches();
            listRepositories();
        }

        System.out.println();
        System.out.println("All Done");

    }

}
