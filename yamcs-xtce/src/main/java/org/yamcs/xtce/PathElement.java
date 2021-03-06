package org.yamcs.xtce;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Describes an element from an aggregate/array member access path
 * For example, the third element from this path : <br>
 * a/c[2]/d[0][5]/x <br>
 * is: <br>
 * name = "d"<br>
 * index = [0, 5]
 *
 */
public class PathElement implements Serializable {
    private static final long serialVersionUID = 1L;
    
    
    final String name;
    final int[] index;

    public PathElement(String name, int[] index) {
        this.name = name;
        this.index = index;
    }

    /**
     * Encodes the path element into a string like
     * 
     * <pre>
     * name[idx_1][idx_2]..[idx_n]
     * </pre>
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (name != null) {
            sb.append(name);
        }
        if (index != null) {
            for (int i = 0; i < index.length; i++) {
                sb.append("[").append(i).append("]");
            }
        }
        return sb.toString();
    }

    /**
     * Creates a path element from a string like
     * 
     * <pre>
     * name[idx_1][idx_2]..[idx_n]
     * </pre>
     */
    public static PathElement fromString(String s) {
        List<Integer> idx = new ArrayList<Integer>();
        int k = s.indexOf('[', 0);
        if (k > 0) {
            String name = s.substring(0, k);
            int k1 = 0;
            while (true) {
                k1 = s.indexOf('[', k1);
                if (k1 == -1) {
                    break;
                }
                int k2 = s.indexOf(']', k1);
                if (k1 == -1) {
                    throw new IllegalArgumentException("Invalid aggregate member path '" + s + "'");
                }
                idx.add(Integer.parseInt(s.substring(k1 + 1, k2)));
                k1 = k2;
            }
            int[] idx1 = null;
            if (!idx.isEmpty()) {
                idx1 = new int[idx.size()];
                for (int u = 0; u < idx.size(); u++) {
                    idx1[u] = idx.get(u);
                }
            }

            return new PathElement(name, idx1);
        } else {
            return new PathElement(s, null);
        }
    }

    public String getName() {
        return name;
    }

    public int[] getIndex() {
        return index;
    }
    
    /**
     * Transforms the path into a string like<br>
     *   a/c[2]/d[0][5]/x <br>
     * 
     * @param path
     * @return
     */
    public static String pathToString(PathElement[] path) {
        if(path == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (PathElement pe : path) {
            if (first) {
                first = false;
            } else {
                sb.append("/");
            }
            sb.append(pe.toString());
        }
        return sb.toString();
    }
}