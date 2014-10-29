package debrepo;

import java.io.File;

import debrepo.repo.DebRepo;

public class Runner {
    public static void main(String[] args) throws RuntimeException {
        File debsDirectory = new File(args[0]);
        File repoDirectory = new File(args[1]);
        DebRepo.RepoBuilder repo = new DebRepo.RepoBuilder(debsDirectory,repoDirectory);
        boolean verbose = (args.length > 2) ? Boolean.valueOf(args[3]) : false;
        repo.verbose(verbose);
        repo.build().execute();
    }

}
