package slimeknights.tconstruct.library.client.book;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.gui.Font;
import org.junit.jupiter.api.Test;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.client.book.data.content.ContentPageIconList;
import slimeknights.mantle.client.book.repository.BookRepository;
import slimeknights.tconstruct.test.BaseMcTest;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class U7BookRegressionTest extends BaseMcTest {
  private static final List<String> BOOKS = List.of("encyclopedia", "tinkers_gadgetry");
  private static final Map<String,Boolean> REFLECTING_SUBTITLE_CASES = Map.of(
    "en_us", true, "zh_cn", true, "es_mx", false, "fr_fr", false, "ja_jp", false,
    "ko_kr", false, "pt_br", false, "ru_ru", false, "zh_tw", false
  );
  private static final List<String> LOCALES = List.of(
    "de_de", "en_us", "es_ar", "es_cl", "es_es", "es_mx", "fr_fr", "id_id", "it_it", "ja_jp",
    "ko_kr", "lzh", "pl_pl", "pt_br", "ru_ru", "sv_se", "tr_tr", "uk_ua", "zh_cn", "zh_tw"
  );

  @Test
  void missingMaterialTierSubtitlesDoNotBreakHtmlAcrossBooksAndLocales() throws IOException {
    List<String> missing = new ArrayList<>();
    int checkedSections = 0;
    int checkedSlimeLocaleCases = 0;

    for (String bookName : BOOKS) {
      JsonArray sections = readIndex(bookName);
      for (String locale : LOCALES) {
        Properties language = readLanguage(bookName, locale);
        if (language == null) {
          if (bookName.equals("encyclopedia") && REFLECTING_SUBTITLE_CASES.containsKey(locale)) {
            assertThat(language).as("Missing expected locale file %s/%s", bookName, locale).isNotNull();
          }
          continue;
        }
        for (var element : sections) {
          JsonObject section = element.getAsJsonObject();
          JsonObject extraData = section.getAsJsonObject("extraData");
          if (extraData == null || !extraData.has("tconstruct:material_tier")) {
            continue;
          }
          String name = section.get("name").getAsString();
          String subText = language.getProperty(name + ".subtext");
          BookData book = new BookData(BookRepository.DUMMY);
          book.fontRenderer = mock(Font.class);
          ContentPageIconList indexPage = new ContentPageIconList();
          indexPage.title = language.getProperty(name, name);
          indexPage.subText = subText;

          StringBuilder htmlBuilder = new StringBuilder();
          indexPage.toHTML(book).toHtml(htmlBuilder, "");
          String html = htmlBuilder.toString();
          checkedSections++;
          if (subText == null) {
            missing.add(bookName + ":" + locale + ":" + name + ".subtext");
            assertThat(html).doesNotContain("<p>");
          } else {
            assertThat(html).contains("<p");
          }

          if (bookName.equals("encyclopedia") && name.equals("materials_slime")) {
            if (REFLECTING_SUBTITLE_CASES.containsKey(locale)) {
              checkedSlimeLocaleCases++;
              assertThat(subText != null).as("%s materials_slime.subtext", locale)
                .isEqualTo(REFLECTING_SUBTITLE_CASES.get(locale));
            }
          }
        }
      }
    }

    assertThat(checkedSections).isPositive();
    assertThat(checkedSlimeLocaleCases).isEqualTo(9);
    assertThat(missing).hasSize(38)
      .contains("encyclopedia:fr_fr:materials_ammo.subtext", "tinkers_gadgetry:ja_jp:melee_harvest_materials.subtext");
  }

  @Test
  void reflectingBookVariantsDescribeDurationAndSlotCostsConsistently() throws IOException {
    List<String> smelting = readEffects(
      "assets/tconstruct/book/mighty_smelting/zh_cn/abilities/armor/shield/tconstruct_reflecting.json");
    List<String> encyclopedia = readEffects(
      "assets/tconstruct/book/encyclopedia/zh_cn/abilities/armor/shield/tconstruct_reflecting.json");
    JsonObject recipe = readObject("data/tconstruct/recipe/tools/modifiers/ability/reflecting.json");

    assertThat(smelting).containsExactly(
      "格挡后2秒内反弹弹射物",
      "多个等级，每级增加2秒判定时间",
      "第1级需要1个能力槽",
      "后续每级需要1个升级槽"
    );
    assertThat(encyclopedia).contains(
      "第1级需要1个能力槽", "后续每级需要1个升级槽", "最高3级"
    );
    assertThat(smelting.stream().filter(line -> line.contains("能力槽"))).hasSize(1);
    assertThat(smelting.stream().filter(line -> line.contains("升级槽"))).hasSize(1);

    JsonArray levels = recipe.getAsJsonArray("levels");
    assertThat(levels).hasSize(2);
    JsonObject firstLevel = levels.get(0).getAsJsonObject();
    assertThat(firstLevel.get("level").getAsInt()).isEqualTo(1);
    assertThat(firstLevel.getAsJsonObject("slots").get("abilities").getAsInt()).isEqualTo(1);
    assertThat(firstLevel.getAsJsonObject("slots").has("upgrades")).isFalse();
    JsonObject laterLevels = levels.get(1).getAsJsonObject();
    assertThat(laterLevels.getAsJsonObject("level").get("min").getAsInt()).isEqualTo(2);
    assertThat(laterLevels.getAsJsonObject("level").get("max").getAsInt()).isEqualTo(3);
    assertThat(laterLevels.getAsJsonObject("slots").get("upgrades").getAsInt()).isEqualTo(1);
    assertThat(laterLevels.getAsJsonObject("slots").has("abilities")).isFalse();
  }

  private static JsonArray readIndex(String book) throws IOException {
    String path = "assets/tconstruct/book/" + book + "/index.json";
    try (InputStream stream = Objects.requireNonNull(
        U7BookRegressionTest.class.getClassLoader().getResourceAsStream(path), "Missing book index: " + path);
         InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
      return JsonParser.parseReader(reader).getAsJsonArray();
    }
  }

  private static Properties readLanguage(String book, String locale) throws IOException {
    String path = "assets/tconstruct/book/" + book + "/" + locale + "/language.lang";
    InputStream stream = U7BookRegressionTest.class.getClassLoader().getResourceAsStream(path);
    if (stream == null) {
      return null;
    }
    Properties properties = new Properties();
    try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    return properties;
  }

  private static List<String> readEffects(String path) throws IOException {
    JsonObject page = readObject(path);
    List<String> effects = new ArrayList<>();
    for (var effect : page.getAsJsonArray("effects")) {
      effects.add(effect.getAsString());
    }
    return effects;
  }

  private static JsonObject readObject(String path) throws IOException {
    try (InputStream stream = Objects.requireNonNull(
        U7BookRegressionTest.class.getClassLoader().getResourceAsStream(path), "Missing resource: " + path);
         InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
      return JsonParser.parseReader(reader).getAsJsonObject();
    }
  }
}
