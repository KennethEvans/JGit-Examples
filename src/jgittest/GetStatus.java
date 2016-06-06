package jgittest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

public class GetStatus
{
  private static final boolean DO_STATUS = true;
  private static final boolean DO_BRANCH_TRACKING = true;
  private static final boolean DO_FULL = true;
  private static final boolean DO_SUMMARY_ONLY = true;

  /** Directories that have a repository (.git directory) in them. */
  private static File[] INDIVIDUAL_REPOSITORIES = {
    // Start
    new File("C:/Git/SVN/AppInfo"), new File("C:/Git/jgit-cookbook"),
    new File("C:/Git/color-thief-java"),
    new File("C:/eclipseProjects/GitWorkspace"),
    // End
  };

  /** The repository locations actually used. */
  private static File[] repositoryLocations;

  /**
   * Directories that have directories with repositories (.git directory) in
   * them.
   */
  private static final File[] PARENT_DIRS = {
    // Start
    new File("C:/AndroidStudioProjects"),
    // new File("C:/Git/SVN"),
    // new File("C:/Git"),
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
    System.out.println("Repositories Status");
    System.out.println("Status=" + doStatus + " BranchTracking="
      + doBranchTracking + " Full=" + full + " SummaryOnly=" + summaryOnly);
    System.out.println();
    
    Repository repository;
    Status status;
    Git git;
    boolean isClean;
    int totalCount = 0;
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
    boolean noTrackingFlag;
    for(File file : repositoryLocations) {
//      if(!summaryOnly) {
        System.out.println(file.getPath());
//      }
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
        totalCount++;

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
          noTrackingFlag = false;
          repository = git.getRepository();
          List<Ref> call = git.branchList().call();
          if(!summaryOnly && call.size() == 0) {
            noTrackingCount++;
            noTrackingFiles.add(file);
            System.out.println("No branches found for " + file.getPath());
          }

          for(Ref ref : call) {
            List<Integer> counts = getTrackingCounts(repository, ref.getName());
            if(counts.get(0) > 0) {
              trackingCountAhead++;
              aheadFiles.add(file);
            }
            if(counts.get(1) > 0) {
              trackingCountBehind++;
              behindFiles.add(file);
            }
            if(counts.get(0) < 0 || counts.get(1) < 0) {
              noTrackingFlag = true;
              noTrackingCount++;
              noTrackingFiles.add(file);
            }
            if(!summaryOnly) {
              if(noTrackingFlag) {
                System.out
                  .println("No tracking found for branch " + ref.getName());
              } else {
                System.out.println("For branch: " + ref.getName());
                System.out.println("Commits ahead : " + counts.get(0));
                System.out.println("Commits behind : " + counts.get(1));
              }
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
    System.out.println("Total: " + totalCount +

      ", Dirty: " + dirtyCount + ", Behind: " + trackingCountBehind
      + ", Ahead: " + trackingCountAhead + ", No tracking: " + noTrackingCount
      + ", Not found: " + notFoundCount);
    if(dirtyCount > 0)

    {
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

  /**
   * List the counts for tracking information.
   * 
   * @param repository
   * @param branchName
   * @return
   * @throws IOException
   */
  private static List<Integer> getTrackingCounts(
    org.eclipse.jgit.lib.Repository repository, String branchName)
    throws IOException {
    BranchTrackingStatus trackingStatus = BranchTrackingStatus.of(repository,
      branchName);
    List<Integer> counts = new ArrayList<>();
    if(trackingStatus != null) {
      counts.add(trackingStatus.getAheadCount());
      counts.add(trackingStatus.getBehindCount());
    } else {
      counts.add(-1);
      counts.add(-1);
    }
    return counts;
  }

  /**
   * Calculates a list of repository locations from the given parent directories
   * and individual repositories.
   * 
   * @param parentDirectories Directories that have directories with
   *          repositories.
   * @param individualRepositories Directories that have repositories.
   * @return The list of repositories.
   */
  public static File[] setRepositoryLocations(File[] parentDirectories,
    File[] individualRepositories) {
    ArrayList<File> newFiles = new ArrayList<>();
    // Directories
    for(File dir : parentDirectories) {
      File[] files = dir.listFiles();
      for(File file : files) {
        if(file.isDirectory()) {
          newFiles.add(file);
        }
      }
    }
    for(File file : individualRepositories) {
      newFiles.add(file);
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
    repositoryLocations = setRepositoryLocations(PARENT_DIRS,
      INDIVIDUAL_REPOSITORIES);

    getStatus(DO_STATUS, DO_BRANCH_TRACKING, DO_FULL, DO_SUMMARY_ONLY);

    System.out.println();
    System.out.println("All Done");

  }

}
