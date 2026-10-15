package slimeknights.tconstruct.library.materials;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.client.book.data.PageData;
import slimeknights.mantle.client.book.data.SectionData;
import slimeknights.mantle.client.book.data.content.ContentPageIconList;
import slimeknights.mantle.client.book.repository.BookRepository;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.fixture.MaterialRegistryFixture;
import slimeknights.tconstruct.fixture.MaterialStatsFixture;
import slimeknights.tconstruct.library.client.book.content.AbstractMaterialContent;
import slimeknights.tconstruct.library.client.book.sectiontransformer.materials.TierRangeMaterialSectionTransformer;
import slimeknights.tconstruct.library.client.book.sectiontransformer.materials.TierRangeMaterialSectionTransformer.MaterialTier;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.materials.definition.TestMaterial;
import slimeknights.tconstruct.library.materials.stats.IMaterialStats;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.materials.MaterialRegistryExtension;
import slimeknights.tconstruct.test.BaseMcTest;
import slimeknights.tconstruct.tools.stats.HandleMaterialStats;
import slimeknights.tconstruct.tools.stats.HeadMaterialStats;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MaterialRegistryExtension.class)
class MaterialTierBookTest extends BaseMcTest {
  private MockedStatic<Minecraft> minecraftMock;
  private static final ResourceLocation HEAD_GROUP = new ResourceLocation("u7_test", "head");
  private static final ResourceLocation HANDLE_GROUP = new ResourceLocation("u7_test", "handle");
  private static final ResourceLocation EMPTY_GROUP = new ResourceLocation("u7_test", "empty");
  private static final ResourceLocation TEST_PAGE = new ResourceLocation("u7_test", "material_page");
  private static final MaterialStatsId EMPTY_STATS = new MaterialStatsId("u7_test", "empty");

  @BeforeAll
  static void registerMaterialGroups() {
    TierRangeMaterialSectionTransformer.registerMaterialType(HEAD_GROUP, TestMaterialPage::new, HeadMaterialStats.ID);
    TierRangeMaterialSectionTransformer.registerMaterialType(HANDLE_GROUP, TestMaterialPage::new, HandleMaterialStats.ID);
    TierRangeMaterialSectionTransformer.registerMaterialType(EMPTY_GROUP, TestMaterialPage::new, EMPTY_STATS);
  }

  @BeforeEach
  void mockMinecraftClientForBookIcons() {
    Minecraft minecraft = mock(Minecraft.class);
    when(minecraft.getTextureManager()).thenReturn(mock(TextureManager.class));
    minecraftMock = mockStatic(Minecraft.class);
    minecraftMock.when(Minecraft::getInstance).thenReturn(minecraft);
  }

  @AfterEach
  void closeMinecraftMock() {
    minecraftMock.close();
  }

  @Test
  void legacySingleObjectKeepsMaterialPageNameAndDefaultsTypeName() {
    MaterialTier tier = MaterialTier.deserialize(JsonParser.parseString("""
      {"type":"u7_test:head","detailed":true,"suffix":"item.u7_test.head"}
      """).getAsJsonObject());

    assertThat(tier.name()).isEqualTo("head");
    assertThat(tier.getMaterials()).extracting(material -> material.getIdentifier().toString())
      .contains("test:mat_head", "test:mat_all_stats");
    assertThat(tier.titleSuffix()).isNotNull();

    SectionData section = section(new BookData(BookRepository.DUMMY));
    tier.createPages(section.parent, section);
    assertThat(section.pages).extracting(PageData::getTitle).contains("test:mat_head", "test:mat_all_stats");
  }

  @Test
  void arrayGroupsKeepSharedMaterialsDistinctAndSkipEmptyGroups() {
    JsonArray tiers = JsonParser.parseString("""
      [
        {"type":"u7_test:head","name":"head","suffix":"item.u7_test.head"},
        {"type":"u7_test:handle","name":"handle","suffix":"item.u7_test.handle"},
        {"type":"u7_test:empty","name":"empty"}
      ]
      """).getAsJsonArray();
    SectionData section = section(new BookData(BookRepository.DUMMY));
    section.extraData = Map.of(TConstruct.getResource("material_tier"), tiers);

    TierRangeMaterialSectionTransformer.INSTANCE.transform(section.parent);

    List<String> pageNames = section.pages.stream().map(page -> page.name).toList();
    assertThat(pageNames).contains("head.test:mat_all_stats", "handle.test:mat_all_stats");
    assertThat(pageNames.stream().filter(name -> name.endsWith("test:mat_all_stats"))).hasSize(2);
    assertThat(pageNames).doesNotContain("empty.test:mat_all_stats");
    assertThat(section.pages.stream().map(page -> page.content).filter(AbstractMaterialContent.class::isInstance)
      .map(AbstractMaterialContent.class::cast).filter(content -> content.titleSuffix != null)).hasSize(4);
  }

  @Test
  void shippedSlimeSuitIndexesUseDetailedAndSummaryTierArrays() throws IOException {
    JsonArray encyclopedia = readMaterialTiers("encyclopedia", "materials_slime");
    JsonArray gadgetry = readMaterialTiers("tinkers_gadgetry", "materials");
    List<String> expectedTypes = List.of(
      "tconstruct:slime", "tconstruct:skull", "tconstruct:ribcage", "tconstruct:shell", "tconstruct:laces");

    assertThat(encyclopedia).hasSize(5);
    assertThat(encyclopedia.asList()).extracting(entry -> entry.getAsJsonObject().get("type").getAsString())
      .containsExactlyElementsOf(expectedTypes);
    assertThat(encyclopedia.asList()).allSatisfy(entry ->
      assertThat(entry.getAsJsonObject().get("detailed").getAsBoolean()).isTrue());
    assertThat(encyclopedia.get(1).getAsJsonObject().get("suffix").getAsString()).isEqualTo("item.tconstruct.skull");
    assertThat(encyclopedia.get(4).getAsJsonObject().get("suffix").getAsString()).isEqualTo("item.tconstruct.laces");

    assertThat(gadgetry).hasSize(5);
    assertThat(gadgetry.asList()).extracting(entry -> entry.getAsJsonObject().get("type").getAsString())
      .containsExactlyElementsOf(expectedTypes);
    assertThat(gadgetry.asList()).allSatisfy(entry ->
      assertThat(entry.getAsJsonObject().has("detailed")).isFalse());
  }

  @Test
  void unknownTypeIsRejectedInsteadOfFallingBackToMelee() {
    JsonObject json = JsonParser.parseString("{\"type\":\"u7_test:missing\"}").getAsJsonObject();

    assertThatThrownBy(() -> MaterialTier.deserialize(json))
      .isInstanceOf(JsonSyntaxException.class)
      .hasMessageContaining("u7_test:missing");
  }

  @Test
  void arrayPaginationCountsAllGroupsBeforeCreatingIndexes() {
    installBulkMaterials(120);
    JsonArray tiers = JsonParser.parseString("""
      [
        {"type":"u7_test:head","name":"head"},
        {"type":"u7_test:handle","name":"handle"}
      ]
      """).getAsJsonArray();
    SectionData section = section(new BookData(BookRepository.DUMMY));
    section.extraData = Map.of(TConstruct.getResource("material_tier"), tiers);

    TierRangeMaterialSectionTransformer.INSTANCE.transform(section.parent);

    List<ContentPageIconList> indexes = section.pages.stream()
      .filter(page -> page.content instanceof ContentPageIconList)
      .map(page -> (ContentPageIconList)page.content).toList();
    assertThat(indexes).hasSizeGreaterThan(1);
    int capacity = indexes.getFirst().getMaxIconCount();
    assertThat(indexes.size() * capacity).isGreaterThanOrEqualTo(240);
    assertThat((indexes.size() - 1) * capacity).isLessThan(240);
    assertThat(section.pages.stream().filter(page -> page.name.startsWith("head.") || page.name.startsWith("handle.")))
      .hasSize(240);
  }

  private static SectionData section(BookData book) {
    SectionData section = new SectionData(true);
    section.name = "materials";
    section.source = BookRepository.DUMMY;
    section.parent = book;
    book.sections.add(section);
    return section;
  }

  private static JsonArray readMaterialTiers(String book, String sectionName) throws IOException {
    String path = "assets/tconstruct/book/" + book + "/index.json";
    InputStream stream = Objects.requireNonNull(
      MaterialTierBookTest.class.getClassLoader().getResourceAsStream(path), "Missing book index: " + path);
    try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
      for (var section : JsonParser.parseReader(reader).getAsJsonArray()) {
        JsonObject sectionObject = section.getAsJsonObject();
        if (sectionName.equals(sectionObject.get("name").getAsString())) {
          return sectionObject.getAsJsonObject("extraData").getAsJsonArray("tconstruct:material_tier");
        }
      }
    }
    Assertions.fail("No material tier section " + sectionName + " in " + path);
    throw new AssertionError("unreachable");
  }

  private static void installBulkMaterials(int count) {
    Map<MaterialId,IMaterial> materials = new HashMap<>();
    Map<MaterialId,Map<MaterialStatsId,IMaterialStats>> stats = new HashMap<>();
    for (int i = 0; i < count; i++) {
      IMaterial material = new TestMaterial(new ResourceLocation("u7_bulk", "material_" + i), true, false);
      materials.put(material.getIdentifier(), material);
      stats.put(material.getIdentifier(), Map.of(
        HeadMaterialStats.ID, MaterialStatsFixture.MATERIAL_STATS_HEAD,
        HandleMaterialStats.ID, MaterialStatsFixture.MATERIAL_STATS_HANDLE
      ));
    }
    MaterialRegistry.INSTANCE = new MaterialRegistry(new MaterialRegistryFixture(
      materials, stats, Map.of(HeadMaterialStats.ID, MaterialStatsFixture.MATERIAL_STATS_HEAD,
      HandleMaterialStats.ID, MaterialStatsFixture.MATERIAL_STATS_HANDLE), Collections.emptyMap()));
    MaterialRegistry.fullyLoaded = true;
  }

  private static class TestMaterialPage extends AbstractMaterialContent {
    private TestMaterialPage(MaterialVariantId material, boolean detailed) {
      super(material, detailed);
    }

    @Override
    public ResourceLocation getId() {
      return TEST_PAGE;
    }

    @Override
    protected MaterialStatsId getStatType(int index) {
      return index == 0 ? HeadMaterialStats.ID : null;
    }

    @Override
    protected boolean supportsStatType(MaterialStatsId statsId) {
      return HeadMaterialStats.ID.equals(statsId) || HandleMaterialStats.ID.equals(statsId);
    }

    @Override
    public List<ItemStack> getDisplayStacks() {
      return Collections.emptyList();
    }

    @Override
    public Component getTitleComponent() {
      return Component.literal(materialName);
    }
  }
}
