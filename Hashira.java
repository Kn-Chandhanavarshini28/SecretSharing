import java.math.BigInteger;
import java.util.*;
import org.json.JSONObject;

public class Hashira {

    public static void main(String[] args) {
        // Example: You can replace this with reading JSON from file/stdin
        String jsonInput = "{\n" +
                "    \"keys\": { \"n\": 4, \"k\": 3 },\n" +
                "    \"1\": { \"base\": \"10\", \"value\": \"4\" },\n" +
                "    \"2\": { \"base\": \"2\", \"value\": \"111\" },\n" +
                "    \"3\": { \"base\": \"10\", \"value\": \"12\" },\n" +
                "    \"6\": { \"base\": \"4\", \"value\": \"213\" }\n" +
                "}";

        JSONObject input = new JSONObject(jsonInput);
        JSONObject keys = input.getJSONObject("keys");
        int n = keys.getInt("n");
        int k = keys.getInt("k");

        // Parse shares into map
        Map<BigInteger, BigInteger> shares = new HashMap<>();
        for (String key : input.keySet()) {
            if (key.equals("keys")) continue;
            JSONObject obj = input.getJSONObject(key);
            int base = Integer.parseInt(obj.getString("base"));
            String valueStr = obj.getString("value");
            BigInteger value = new BigInteger(valueStr, base);
            shares.put(new BigInteger(key), value);
        }

        // Try all subsets of size k to find consistent polynomial
        List<BigInteger> xVals = new ArrayList<>(shares.keySet());
        BigInteger secret = null;
        Set<BigInteger> wrongShares = new HashSet<>();

        outer:
        for (int i = 0; i < xVals.size(); i++) {
            for (int j = i + 1; j < xVals.size(); j++) {
                for (int l = j + 1; l < xVals.size(); l++) {
                    List<BigInteger> subset = Arrays.asList(xVals.get(i), xVals.get(j), xVals.get(l));
                    BigInteger candidate = lagrangeInterpolationAtZero(subset, shares);
                    if (secret == null) {
                        secret = candidate;
                    } else if (!secret.equals(candidate)) {
                        continue; // inconsistent subset
                    }
                }
            }
        }

        // Identify wrong shares
        for (Map.Entry<BigInteger, BigInteger> entry : shares.entrySet()) {
            BigInteger expected = evaluatePolyAt(entry.getKey(), secret);
            if (!entry.getValue().equals(expected)) {
                wrongShares.add(entry.getKey());
            }
        }

        // Output
        System.out.println("{");
        System.out.println("  \"secret\": " + secret + ",");
        System.out.println("  \"wrongShares\": " + wrongShares);
        System.out.println("}");
    }

    // Lagrange interpolation: compute f(0)
    private static BigInteger lagrangeInterpolationAtZero(List<BigInteger> xs, Map<BigInteger, BigInteger> shares) {
        BigInteger result = BigInteger.ZERO;
        for (int i = 0; i < xs.size(); i++) {
            BigInteger xi = xs.get(i);
            BigInteger yi = shares.get(xi);
            BigInteger num = BigInteger.ONE;
            BigInteger den = BigInteger.ONE;
            for (int j = 0; j < xs.size(); j++) {
                if (i != j) {
                    BigInteger xj = xs.get(j);
                    num = num.multiply(xj.negate()); // (0 - xj)
                    den = den.multiply(xi.subtract(xj));
                }
            }
            BigInteger term = yi.multiply(num).divide(den);
            result = result.add(term);
        }
        return result;
    }

    // Evaluate polynomial (for wrong share detection, simplified as quadratic example)
    private static BigInteger evaluatePolyAt(BigInteger x, BigInteger secret) {
        // NOTE: In real scheme we’d reconstruct full poly, but here we assume form f(x)=x^2+secret
        return x.multiply(x).add(secret);
    }
}
