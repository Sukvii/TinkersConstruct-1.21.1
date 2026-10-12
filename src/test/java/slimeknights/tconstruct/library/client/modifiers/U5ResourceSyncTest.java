package slimeknights.tconstruct.library.client.modifiers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.test.BaseMcTest;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Fixed-upstream regression checks for the U5 resource migration (pixel content, not PNG encoding). */
class U5ResourceSyncTest extends BaseMcTest {
  private static Path projectRoot() {
    for (Path path = Path.of(System.getProperty("user.dir")).toAbsolutePath(); path != null; path = path.getParent()) {
      if (Files.isDirectory(path.resolve("src/main/resources")) && Files.isDirectory(path.resolve("src/generated/resources"))) {
        return path;
      }
    }
    throw new IllegalStateException("could not locate Tinkers' Construct project resources from " + System.getProperty("user.dir"));
  }

  private static JsonObject expectations() throws IOException {
    try (var input = U5ResourceSyncTest.class.getResourceAsStream("/u5-upstream-resource-expectations.json")) {
      assertThat(input).as("locked U5 upstream expectation fixture").isNotNull();
      return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
    }
  }

  private static String pixelHash(Path path) throws IOException, NoSuchAlgorithmException {
    var image = ImageIO.read(path.toFile());
    assertThat(image).as("read PNG %s", path).isNotNull();
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    digest.update(ByteBuffer.allocate(Integer.BYTES * 2).putInt(image.getWidth()).putInt(image.getHeight()).array());
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        int argb = image.getRGB(x, y);
        digest.update((byte)(argb >>> 16));
        digest.update((byte)(argb >>> 8));
        digest.update((byte)argb);
        digest.update((byte)(argb >>> 24));
      }
    }
    return HexFormat.of().formatHex(digest.digest());
  }

  @Test
  void allU5ItemModelsMatchLockedUpstreamWithNeoForgeParentAdaptation() throws IOException {
    JsonObject models = expectations().getAsJsonObject("models");
    assertThat(models.size()).isEqualTo(59);
    for (Map.Entry<String, com.google.gson.JsonElement> entry : models.entrySet()) {
      Path path = projectRoot().resolve(entry.getKey());
      assertThat(JsonParser.parseString(Files.readString(path)))
        .as("U5 model %s", path).isEqualTo(entry.getValue());
    }
  }

  @Test
  void staticU5JsonMatchesLockedUpstream() throws IOException {
    JsonObject resources = expectations().getAsJsonObject("static_json");
    assertThat(resources.size()).isEqualTo(3);
    for (Map.Entry<String, com.google.gson.JsonElement> entry : resources.entrySet()) {
      Path path = projectRoot().resolve(entry.getKey());
      assertThat(JsonParser.parseString(Files.readString(path)))
        .as("U5 static resource %s", path).isEqualTo(entry.getValue());
    }
  }

  @Test
  void migratedU5PngsKeepLockedUpstreamPixels() throws IOException, NoSuchAlgorithmException {
    JsonObject pngs = expectations().getAsJsonObject("png");
    assertThat(pngs.size()).isEqualTo(92);
    for (Map.Entry<String, com.google.gson.JsonElement> entry : pngs.entrySet()) {
      Path path = projectRoot().resolve(entry.getKey());
      JsonObject expected = entry.getValue().getAsJsonObject();
      var image = ImageIO.read(path.toFile());
      assertThat(image).as("U5 PNG %s", path).isNotNull();
      assertThat(image.getWidth()).as("width %s", path).isEqualTo(expected.get("width").getAsInt());
      assertThat(image.getHeight()).as("height %s", path).isEqualTo(expected.get("height").getAsInt());
      assertThat(pixelHash(path)).as("decoded pixels %s", path).isEqualTo(expected.get("pixel_sha256").getAsString());
    }
  }

  @Test
  void renamedUnknownShellTexturesExistOnlyAtTheirMainResourcePaths() {
    for (String name : new String[] {"shell_tconstruct_unknown.png", "shell_broken_tconstruct_unknown.png"}) {
      Path main = projectRoot().resolve("src/main/resources/assets/tconstruct/textures/item/tool/armor/slime/leggings/" + name);
      Path generated = projectRoot().resolve("src/generated/resources/assets/tconstruct/textures/item/tool/armor/slime/leggings/" + name);
      assertThat(Files.isRegularFile(main)).as("renamed main texture %s", name).isTrue();
      assertThat(Files.exists(generated)).as("obsolete generated texture %s", name).isFalse();
    }
  }
}
