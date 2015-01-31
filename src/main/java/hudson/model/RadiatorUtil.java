package hudson.model;

import hudson.tasks.Mailer;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;

/**
 * Utilities used by the radiator view. 
 */
public class RadiatorUtil {

	public static Result getLastFinishedResult(Job job) {
		Run lastBuild = job.getLastBuild();
		while (lastBuild != null
				&& (lastBuild.hasntStartedYet() || lastBuild.isBuilding()
						|| lastBuild.isLogUpdated() || lastBuild.getResult() == Result.ABORTED)) {
			lastBuild = lastBuild.getPreviousBuild();
		}
		if (lastBuild != null) {
			return lastBuild.getResult();
		} else {
			return Result.NOT_BUILT;
		}
	}

    public static String getGravatar(Collection<User> culprits) {
        if (culprits.isEmpty()) {
            return null;
        }
        String hash = "00000000000000000000000000000000";
        try {
            User user = culprits.iterator().next();
            String email = user.getProperty(Mailer.UserProperty.class).getAddress().trim().toLowerCase();
            hash = md5(email);
        } catch (Exception e) {
        }
        return "http://www.gravatar.com/avatar/"+hash+"?s=320&d=monsterid";
    }

    private static String md5(String string) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] digest = md5.digest(string.getBytes("UTF-8"));
        BigInteger bigInt = new BigInteger(1,digest);
        String hashtext = bigInt.toString(16);
        // Now we need to zero pad it if you actually want the full 32 chars.
        while(hashtext.length() < 32 ){
            hashtext = "0"+hashtext;
        }
        return hashtext;
    }
}
