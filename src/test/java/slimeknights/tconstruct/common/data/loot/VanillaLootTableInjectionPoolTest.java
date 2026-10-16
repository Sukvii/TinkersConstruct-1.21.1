package slimeknights.tconstruct.common.data.loot;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** Guards vanilla loot injections against the default pool names assigned by NeoForge 21.1. */
class VanillaLootTableInjectionPoolTest {
  private static Path projectRoot() {
    for (Path path = Path.of(System.getProperty("user.dir")).toAbsolutePath(); path != null; path = path.getParent()) {
      if (Files.isDirectory(path.resolve("src/main/resources")) && Files.isDirectory(path.resolve("src/generated/resources"))) {
        return path;
      }
    }
    throw new IllegalStateException("could not locate Tinkers' Construct project resources from " + System.getProperty("user.dir"));
  }

  @Test
  void generatedVanillaInjectionsUseNamesAssignedToTheTargetLootPools() throws IOException {
    Path root = projectRoot();
    Path resources = root.resolve("src/generated/resources/data/tconstruct/mantle/loot_injectors");
    Path artifacts = root.resolve("build/moddev/artifacts");
    List<Path> resourceJars;
    try (Stream<Path> files = Files.list(artifacts)) {
      resourceJars = files.filter(path -> path.getFileName().toString().endsWith("-client-extra-aka-minecraft-resources.jar"))
        .toList();
    }
    assertThat(resourceJars).as("NeoForge Minecraft resource JAR").hasSize(1);

    int checked = 0;
    try (ZipFile minecraft = new ZipFile(resourceJars.get(0).toFile()); Stream<Path> files = Files.walk(resources)) {
      for (Path file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
        JsonObject injection = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        String tableId = injection.get("name").getAsString();
        String[] id = tableId.split(":", 2);
        if (id.length != 2 || !id[0].equals("minecraft")) {
          continue;
        }

        String tablePath = "data/minecraft/loot_table/" + id[1] + ".json";
        var tableEntry = minecraft.getEntry(tablePath);
        assertThat(tableEntry).as("vanilla loot table %s referenced by %s", tableId, file).isNotNull();
        JsonObject table;
        try (var input = minecraft.getInputStream(tableEntry)) {
          table = JsonParser.parseReader(new java.io.InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        }

        JsonArray pools = table.getAsJsonArray("pools");
        List<String> availableNames = new ArrayList<>();
        for (int i = 0; i < pools.size(); i++) {
          JsonObject pool = pools.get(i).getAsJsonObject();
          availableNames.add(pool.has("name")
            ? pool.get("name").getAsString()
            : pools.size() == 1 ? "main" : "pool" + i);
        }

        for (JsonElement pool : injection.getAsJsonArray("pools")) {
          String requestedName = pool.getAsJsonObject().get("name").getAsString();
          assertThat(availableNames)
            .as("injection %s targets %s; available NeoForge pool names are %s", file, tableId, availableNames)
            .contains(requestedName);
          checked++;
        }
      }
    }
    assertThat(checked).as("vanilla loot injection pools checked").isGreaterThan(0);
  }
}
