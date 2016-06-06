package jgittest;

import java.io.File;
import java.util.Iterator;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.AbortedByHookException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

class Main
{
    public static void main(String args[]) {
        String name = "Saito";
        String password = "a1c2bf1890eb";
        String url = "http://localhost:9292/Saito/simba.git";

        // credentials
        CredentialsProvider cp = new UsernamePasswordCredentialsProvider(name,
            password);

        // clone
        File dir = new File("/tmp/abc");
        CloneCommand cc = new CloneCommand().setCredentialsProvider(cp)
            .setDirectory(dir).setURI(url);
        Git git = null;
        ;
        try {
            git = cc.call();
        } catch(GitAPIException ex1) {
            // TODO Auto-generated catch block
            ex1.printStackTrace();
        }

        // add
        AddCommand ac = git.add();
        ac.addFilepattern(".");
        try {
            ac.call();
        } catch(NoFilepatternException e) {
            e.printStackTrace();
        } catch(GitAPIException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }

        // commit
        CommitCommand commit = git.commit();
        commit.setCommitter("TMall", "open@tmall.com").setMessage("push war");
        try {
            commit.call();
        } catch(NoHeadException e) {
            e.printStackTrace();
        } catch(NoMessageException e) {
            e.printStackTrace();
        } catch(ConcurrentRefUpdateException e) {
            e.printStackTrace();
        } catch(WrongRepositoryStateException e) {
            e.printStackTrace();
        } catch(UnmergedPathsException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        } catch(AbortedByHookException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        } catch(GitAPIException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }
        
        // push
        PushCommand pc = git.push();
        pc.setCredentialsProvider(cp).setForce(true).setPushAll();
        try {
            Iterator<PushResult> it = pc.call().iterator();
            if(it.hasNext()) {
                System.out.println(it.next().toString());
            }
        } catch(InvalidRemoteException e) {
            e.printStackTrace();
        } catch(TransportException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        } catch(GitAPIException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }

        // cleanup
        dir.deleteOnExit();
    }
}