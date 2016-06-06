package jgittest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

public class JGitTest
{
  public static final boolean DO = true;
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
              System.out.println("Changed: " + status.getChanged());
              System.out.println("Conflicting: " + status.getConflicting());
              System.out.println(
                "ConflictingStageState: " + status.getConflictingStageState());
              System.out
                .println("IgnoredNotInIndex: " + status.getIgnoredNotInIndex());
              System.out.println("Missing: " + status.getMissing());
              System.out.println("Modified: " + status.getModified());
              System.out.println("Removed: " + status.getRemoved());
              System.out.println("Untracked: " + status.getUntracked());
              System.out
                .println("UntrackedFolders: " + status.getUntrackedFolders());
            }
          }
        }

        // Branch tracking
        if(doBranchTracking) {
          repository = git.getRepository();
          List<Ref> call = git.branchList().call();
          for(Ref ref : call) {
            List<Integer> counts = getTrackingCounts(repository, ref.getName(),
              !summaryOnly);
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
              System.out.println("Commits ahead : " + counts.get(0));
              System.out.println("Commits behind : " + counts.get(1));
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
    System.out.println("Dirty: " + dirtyCount + ", Behind: "
      + trackingCountBehind + ", Ahead: " + trackingCountAhead
      + ", No tracking: " + noTrackingCount + ", Not found: " + notFoundCount);
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
          List<Integer> counts = getTrackingCounts(repository, ref.getName(),
            true);
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
          System.out.println("Conflicting: " + status.getConflicting());
          System.out.println(
            "ConflictingStageState: " + status.getConflictingStageState());
          System.out
            .println("IgnoredNotInIndex: " + status.getIgnoredNotInIndex());
          System.out.println("Missing: " + status.getMissing());
          System.out.println("Modified: " + status.getModified());
          System.out.println("Removed: " + status.getRemoved());
          System.out.println("Untracked: " + status.getUntracked());
          System.out
            .println("UntrackedFolders: " + status.getUntrackedFolders());
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
            String url = storedConfig.getString("remote", remoteName, "url");
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
          System.out.println("Branch: " + ref + " " + ref.getName() + " "
            + ref.getObjectId().getName());
        }

        System.out.println("Now including remote branches:");
        call = git.branchList().setListMode(ListMode.ALL).call();
        for(Ref ref : call) {
          System.out.println("Branch: " + ref + " " + ref.getName() + " "
            + ref.getObjectId().getName());
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
          List<Integer> counts = getTrackingCounts(repository, ref.getName(),
            true);
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
    BranchTrackingStatus trackingStatus = BranchTrackingStatus.of(repository,
      branchName);
    List<Integer> counts = new ArrayList<>();
    if(trackingStatus != null) {
      // System.out.println("Remote tracking branch " +
      // trackingStatus.getRemoteTrackingBranch());
      counts.add(trackingStatus.getAheadCount());
      counts.add(trackingStatus.getBehindCount());
    } else {
      if(doPrint) {
        System.out.println(
          "There is likely no remote tracking of branch " + branchName);
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
   * @param parents The parent directory that have repository locations in them.
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
      getStatus(true, true, false, true);
    } else {
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
