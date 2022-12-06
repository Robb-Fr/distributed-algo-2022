package cs451.Parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigParser {

    private String path;

    public boolean populate(String value) {
        File file = new File(value);
        path = file.getPath();
        return true;
    }

    public String getPath() {
        return path;
    }

    public LatticeConfig getLatticeConfig() {
        try {
            BufferedReader myReader = new BufferedReader(new FileReader(path));
            List<String[]> data = new ArrayList<>();
            String[] line = null;
            while ((line = myReader.readLine().split(" ")) != null) {
                data.add(line);
            }
            myReader.close();
            if (data != null && data.get(0) != null && data.get(0).length == 3) {
                int p = Integer.parseInt(data.get(0)[0]);
                int vs = Integer.parseInt(data.get(0)[1]);
                int ds = Integer.parseInt(data.get(0)[2]);
                if (data.size() - 1 != p) {
                    throw new IllegalArgumentException("Config does not contain the expected number of proposals");
                }
                List<Set<Integer>> proposals = new ArrayList<>(p);
                for (int i = 1; i < p + 1; ++i) {
                    if (data.get(i).length <= vs) {
                        int nbProposal = data.get(i).length;
                        Set<Integer> proposal = new HashSet<>(nbProposal);
                        for (int j = 0; j < nbProposal; ++i) {
                            proposal.add(Integer.parseInt(data.get(i)[j]));
                        }
                        proposals.add(proposal);
                    } else {
                        throw new IllegalArgumentException("Config contains for line " + i + " more messages than vs");
                    }
                }
                return new LatticeConfig(p, vs, ds, proposals);
            }
        } catch (NumberFormatException e) {
            System.err.println("Error occurred parse the parameters of the config");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("An error occurred while reading the config file");
            e.printStackTrace();
        }
        return null;
    }

    public class LatticeConfig {
        private final int p;
        private final int vs;
        private final int ds;
        private final List<Set<Integer>> proposals;

        public LatticeConfig(int p, int vs, int ds, List<Set<Integer>> proposals) {
            if (proposals == null) {
                throw new IllegalArgumentException("Cannot have null proposals");
            }
            if (proposals.size() != p) {
                throw new IllegalArgumentException("Incorrect value for p and proposals size");
            }
            this.p = p;
            this.vs = vs;
            this.ds = ds;
            this.proposals = proposals;
        }

        public int getP() {
            return p;
        }

        public int getVs() {
            return vs;
        }

        public int getDs() {
            return ds;
        }

        public List<Set<Integer>> getProposals() {
            return proposals;
        }

        @Override
        public String toString() {
            return "LatticeConfig [p=" + p + ", vs=" + vs + ", ds=" + ds + ", proposals=" + proposals + "]";
        }
    }

}
