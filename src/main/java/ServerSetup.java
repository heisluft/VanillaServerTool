import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

public class ServerSetup {
  public static void main(String[] args) throws IOException, ParseException, NoSuchAlgorithmException {
    if (args.length < 2) {
      System.out.println("Usage: setupServer <directory> <version>");
      return;
    }
    Path dir = Path.of(args[0]);
    if (!Files.isDirectory(dir)) {
      System.err.println(args[0] + " is not a directory");
      return;
    }
    InputStreamReader manifestReader = new InputStreamReader(new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json").openConnection().getInputStream());
    JSONObject versionManifest = (JSONObject) new JSONParser().parse(manifestReader);
    manifestReader.close();
    if ("latest".equals(args[1]) || "latest-snapshot".equals(args[1])) {
      args[1] = versionManifest.getObject("latest").getString(args[1].equals("latest") ? "release" : "snapshot");
    }
    Optional<JSONObject> queriedVersion = versionManifest.getArray("versions").stream().map(JSONObject.class::cast).filter(o -> o.getString("id").equals(args[1])).findFirst();
    if (queriedVersion.isEmpty()) {
      System.out.println("Version '" + args[1] + "' could not be found.");
      return;
    }
    try (InputStreamReader versionReader = new InputStreamReader(new URL(queriedVersion.get().getString("url")).openStream())) {
      JSONObject serverDownload = ((JSONObject) new JSONParser().parse(versionReader)).getObject("downloads").getObject("server");
      try (InputStream serverJarInputStream = new URL(serverDownload.getString("url")).openStream()) {
        byte[] serverJarBytes = serverJarInputStream.readAllBytes();
        StringBuilder calcSha1 = new StringBuilder();
        byte[] sha1Bytes = MessageDigest.getInstance("SHA-1").digest(serverJarBytes);
        for (byte b : sha1Bytes) {
          calcSha1.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }
        if (!calcSha1.toString().equals(serverDownload.getString("sha1"))) {
          System.err.println("Download failed, checksum mismatch. Expected '" + serverDownload.getString("sha1") + "' but got '" + calcSha1.toString() + "'. Please try again");
          return;
        }
        Files.write(dir.resolve("server.jar"), serverJarBytes);
        Files.write(dir.resolve("eula.txt"), "eula=true".getBytes(StandardCharsets.UTF_8));
      }
    }
  }
}
