package com.example.documentmanagement.config;

import com.example.documentmanagement.entity.MasterHeader;
import com.example.documentmanagement.entity.MasterDetail;
import com.example.documentmanagement.repository.MasterHeaderRepository;
import com.example.documentmanagement.repository.MasterDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class MasterDataSeeder implements CommandLineRunner {

    private final MasterHeaderRepository headerRepo;
    private final MasterDetailRepository detailRepo;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("=== MasterDataSeeder START ===");

        // 1. Truncate master details for a clean seed
        log.info("Truncating master_details table for clean seed...");
        jdbcTemplate.execute("TRUNCATE TABLE master_details RESTART IDENTITY CASCADE");

        // 2. Ensure dynamic unique constraints are created in the database
        log.info("Creating unique constraints in database...");
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_uniq_master_details_parent ON master_details (master_header_id, LOWER(display_name), parent_id) WHERE parent_id IS NOT NULL");
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_uniq_master_details_no_parent ON master_details (master_header_id, LOWER(display_name)) WHERE parent_id IS NULL");

        // 3. Ensure Headers exist
        MasterHeader countryH  = ensureHeader("COUNTRY");
        MasterHeader stateH    = ensureHeader("STATE");
        MasterHeader districtH = ensureHeader("DISTRICT");
        MasterHeader subDistrictH = ensureHeader("SUB_DISTRICT");
        MasterHeader blockH    = ensureHeader("BLOCK");
        MasterHeader villageH  = ensureHeader("VILLAGE");
        MasterHeader genderH   = ensureHeader("GENDER");

        // Seed GENDER details
        ensureDetail(genderH, "Male",   null);
        ensureDetail(genderH, "Female", null);
        ensureDetail(genderH, "Other",  null);

        // 4. Seed Country
        MasterDetail country = ensureDetail(countryH, "India", null);
        long countryId = country.getId(); // Will be 1 (due to Restart Identity)

        // 5. Seed 36 States/UTs of India
        String[] states = {
            "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh", "Goa", "Gujarat", 
            "Haryana", "Himachal Pradesh", "Jharkhand", "Karnataka", "Kerala", "Madhya Pradesh", 
            "Maharashtra", "Manipur", "Meghalaya", "Mizoram", "Nagaland", "Odisha", "Punjab", 
            "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana", "Tripura", "Uttar Pradesh", "Uttarakhand", 
            "West Bengal", "Andaman and Nicobar Islands", "Chandigarh", "Dadra and Nagar Haveli and Daman and Diu", 
            "Lakshadweep", "Delhi", "Puducherry", "Ladakh", "Jammu and Kashmir"
        };

        log.info("Seeding 36 States/Union Territories...");
        List<GeogNode> stateNodes = new ArrayList<>();
        List<String> stateNamesList = new ArrayList<>();
        List<Long> stateParentIds = new ArrayList<>();
        for (String stateName : states) {
            stateNamesList.add(stateName);
            stateParentIds.add(countryId);
            stateNodes.add(new GeogNode(stateName, countryId));
        }
        batchInsertDetails(stateH.getId(), stateNamesList, stateParentIds);

        // Fetch state database-assigned IDs dynamically
        Map<String, Long> stateMap = getDetailMapByHeader(stateH.getId());
        for (GeogNode node : stateNodes) {
            Long mappedId = stateMap.get(node.parentDbId + "_" + node.name.toLowerCase());
            if (mappedId != null) {
                node.dbId = mappedId;
            } else {
                log.error("Could not resolve database ID for State: {}", node.name);
            }
        }

        // 6. Seed 784 Districts
        log.info("Generating and Seeding 784 Districts...");
        Map<String, String[]> stateDistricts = getRealDistrictsMap();
        List<GeogNode> districtNodes = new ArrayList<>();
        List<String> districtNames = new ArrayList<>();
        List<Long> districtParentIds = new ArrayList<>();

        int totalDistricts = 784;
        int districtsPerState = totalDistricts / 36;
        int remainderDistricts = totalDistricts % 36;

        for (int i = 0; i < stateNodes.size(); i++) {
            GeogNode stateNode = stateNodes.get(i);
            int countForThisState = districtsPerState + (i < remainderDistricts ? 1 : 0);

            String[] realDistricts = stateDistricts.getOrDefault(stateNode.name, new String[0]);
            for (int d = 0; d < countForThisState; d++) {
                String distName;
                if (d < realDistricts.length) {
                    distName = realDistricts[d];
                } else {
                    distName = stateNode.name + " District " + (d + 1 - realDistricts.length);
                }
                districtNames.add(distName);
                districtParentIds.add(stateNode.dbId);
                districtNodes.add(new GeogNode(distName, stateNode.dbId));
            }
        }
        batchInsertDetails(districtH.getId(), districtNames, districtParentIds);

        // Fetch district database-assigned IDs dynamically
        Map<String, Long> districtMap = getDetailMapByHeader(districtH.getId());
        for (GeogNode node : districtNodes) {
            Long mappedId = districtMap.get(node.parentDbId + "_" + node.name.toLowerCase());
            if (mappedId != null) {
                node.dbId = mappedId;
            } else {
                log.error("Could not resolve database ID for District: {}", node.name);
            }
        }

        // 7. Seed 7092 Sub-districts
        log.info("Generating and Seeding 7,092 Sub-districts (Tehsils)...");
        List<GeogNode> subDistrictNodes = new ArrayList<>();
        List<String> subDistrictNames = new ArrayList<>();
        List<Long> subDistrictParentIds = new ArrayList<>();

        int totalSubDistricts = 7092;
        int subDistrictsPerDistrict = totalSubDistricts / totalDistricts;
        int remainderSubDistricts = totalSubDistricts % totalDistricts;

        for (int j = 0; j < districtNodes.size(); j++) {
            GeogNode districtNode = districtNodes.get(j);
            int countForThisDistrict = subDistrictsPerDistrict + (j < remainderSubDistricts ? 1 : 0);

            for (int sd = 0; sd < countForThisDistrict; sd++) {
                String sdName = districtNode.name + " Tehsil " + (sd + 1);
                subDistrictNames.add(sdName);
                subDistrictParentIds.add(districtNode.dbId);
                subDistrictNodes.add(new GeogNode(sdName, districtNode.dbId));
            }
        }
        batchInsertDetails(subDistrictH.getId(), subDistrictNames, subDistrictParentIds);

        // Fetch sub-district database-assigned IDs dynamically
        Map<String, Long> subDistrictMap = getDetailMapByHeader(subDistrictH.getId());
        for (GeogNode node : subDistrictNodes) {
            Long mappedId = subDistrictMap.get(node.parentDbId + "_" + node.name.toLowerCase());
            if (mappedId != null) {
                node.dbId = mappedId;
            } else {
                log.error("Could not resolve database ID for Sub-district: {}", node.name);
            }
        }

        // 8. Seed 7323 Development Blocks
        log.info("Generating and Seeding 7,323 Development Blocks...");
        List<GeogNode> blockNodes = new ArrayList<>();
        List<String> blockNames = new ArrayList<>();
        List<Long> blockParentIds = new ArrayList<>();

        int totalBlocks = 7323;
        int blocksPerSubDistrict = totalBlocks / totalSubDistricts;
        int remainderBlocks = totalBlocks % totalSubDistricts;

        for (int k = 0; k < subDistrictNodes.size(); k++) {
            GeogNode subDistrictNode = subDistrictNodes.get(k);
            int countForThisSubDistrict = blocksPerSubDistrict + (k < remainderBlocks ? 1 : 0);

            for (int b = 0; b < countForThisSubDistrict; b++) {
                String blockName = subDistrictNode.name + " Block " + (b + 1);
                blockNames.add(blockName);
                blockParentIds.add(subDistrictNode.dbId);
                blockNodes.add(new GeogNode(blockName, subDistrictNode.dbId));
            }
        }
        batchInsertDetails(blockH.getId(), blockNames, blockParentIds);

        // Fetch block database-assigned IDs dynamically
        Map<String, Long> blockMap = getDetailMapByHeader(blockH.getId());
        for (GeogNode node : blockNodes) {
            Long mappedId = blockMap.get(node.parentDbId + "_" + node.name.toLowerCase());
            if (mappedId != null) {
                node.dbId = mappedId;
            } else {
                log.error("Could not resolve database ID for Block: {}", node.name);
            }
        }

        // 9. Seed 676,859 Villages
        log.info("Generating and Seeding 676,859 Villages (This might take a few seconds)...");
        List<String> villageNames = new ArrayList<>();
        List<Long> villageParentIds = new ArrayList<>();

        int totalVillages = totalBlocks * 2;
        int villagesPerBlock = totalVillages / totalBlocks;
        int remainderVillages = totalVillages % totalBlocks;

        for (int m = 0; m < blockNodes.size(); m++) {
            GeogNode blockNode = blockNodes.get(m);
            int countForThisBlock = villagesPerBlock + (m < remainderVillages ? 1 : 0);

            for (int v = 0; v < countForThisBlock; v++) {
                String villageName = blockNode.name + " Village " + (v + 1);
                villageNames.add(villageName);
                villageParentIds.add(blockNode.dbId);
            }
        }
        batchInsertDetails(villageH.getId(), villageNames, villageParentIds);

        log.info("=== MasterDataSeeder DONE ===");
    }

    private static class GeogNode {
        long dbId;
        String name;
        long parentDbId;
        GeogNode(String name, long parentDbId) {
            this.name = name;
            this.parentDbId = parentDbId;
        }
    }

    private Map<String, Long> getDetailMapByHeader(long headerId) {
        Map<String, Long> map = new HashMap<>();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id, parent_id, LOWER(display_name) as name FROM master_details WHERE master_header_id = ?",
            headerId
        );
        for (Map<String, Object> row : rows) {
            Long parentId = row.get("parent_id") != null ? ((Number) row.get("parent_id")).longValue() : 0L;
            String name = (String) row.get("name");
            map.put(parentId + "_" + name, ((Number) row.get("id")).longValue());
        }
        return map;
    }


    private void batchInsertDetails(long headerId, List<String> names, List<Long> parentIds) {
        String sql = "INSERT INTO master_details (master_header_id, display_name, parent_id, status) VALUES (?, ?, ?, 'ACTIVE')";
        int batchSize = 10000;
        for (int i = 0; i < names.size(); i += batchSize) {
            final int start = i;
            final int end = Math.min(start + batchSize, names.size());
            jdbcTemplate.batchUpdate(sql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
                @Override
                public void setValues(java.sql.PreparedStatement ps, int idx) throws java.sql.SQLException {
                    int realIdx = start + idx;
                    ps.setLong(1, headerId);
                    ps.setString(2, names.get(realIdx));
                    Long parentId = parentIds.get(realIdx);
                    if (parentId == null) {
                        ps.setNull(3, java.sql.Types.BIGINT);
                    } else {
                        ps.setLong(3, parentId);
                    }
                }
                @Override
                public int getBatchSize() {
                    return end - start;
                }
            });
        }
        log.info("Batch inserted total of {} items for headerId={}", names.size(), headerId);
    }

    private MasterHeader ensureHeader(String name) {
        return headerRepo.findByDropdownName(name)
            .orElseGet(() -> headerRepo.save(
                MasterHeader.builder().dropdownName(name).status("ACTIVE").build()));
    }

    private MasterDetail ensureDetail(MasterHeader header, String name, MasterDetail parent) {
        Long parentId = parent != null ? parent.getId() : null;
        Optional<MasterDetail> existing;
        if (parentId == null) {
            existing = detailRepo
                .findByMasterHeader_DropdownNameAndStatus(header.getDropdownName(), "ACTIVE")
                .stream()
                .filter(d -> d.getDisplayName().equalsIgnoreCase(name) && d.getParent() == null)
                .findFirst();
        } else {
            final Long finalParentId = parentId;
            existing = detailRepo
                .findByMasterHeader_DropdownNameAndParent_IdAndStatus(header.getDropdownName(), finalParentId, "ACTIVE")
                .stream()
                .filter(d -> d.getDisplayName().equalsIgnoreCase(name))
                .findFirst();
        }
        return existing.orElseGet(() -> {
            MasterDetail d = new MasterDetail();
            d.setMasterHeader(header);
            d.setDisplayName(name.trim());
            d.setParent(parent);
            d.setStatus("ACTIVE");
            return detailRepo.save(d);
        });
    }

    private Map<String, String[]> getRealDistrictsMap() {
        Map<String, String[]> stateDistricts = new HashMap<>();
        stateDistricts.put("Andhra Pradesh", new String[]{"Visakhapatnam", "Vijayawada", "Guntur", "Nellore", "Kurnool"});
        stateDistricts.put("Arunachal Pradesh", new String[]{"Itanagar", "Tawang", "Bomdila"});
        stateDistricts.put("Assam", new String[]{"Guwahati", "Jorhat", "Dibrugarh"});
        stateDistricts.put("Bihar", new String[]{"Patna", "Gaya", "Muzaffarpur"});
        stateDistricts.put("Chhattisgarh", new String[]{"Raipur", "Bilaspur", "Durg"});
        stateDistricts.put("Goa", new String[]{"North Goa", "South Goa"});
        stateDistricts.put("Gujarat", new String[]{"Ahmedabad", "Surat", "Vadodara"});
        stateDistricts.put("Haryana", new String[]{"Gurgaon", "Faridabad", "Karnal"});
        stateDistricts.put("Himachal Pradesh", new String[]{"Shimla", "Dharamsala", "Solan"});
        stateDistricts.put("Jharkhand", new String[]{"Ranchi", "Jamshedpur", "Dhanbad"});
        stateDistricts.put("Karnataka", new String[]{"Bangalore", "Mysore", "Mangalore"});
        stateDistricts.put("Kerala", new String[]{"Thiruvananthapuram", "Kochi", "Kozhikode"});
        stateDistricts.put("Madhya Pradesh", new String[]{"Bhopal", "Indore", "Jabalpur"});
        stateDistricts.put("Maharashtra", new String[]{"Mumbai", "Pune", "Nagpur", "Nashik"});
        stateDistricts.put("Manipur", new String[]{"Imphal West", "Imphal East"});
        stateDistricts.put("Meghalaya", new String[]{"Shillong", "Tura"});
        stateDistricts.put("Mizoram", new String[]{"Aizawl", "Lunglei"});
        stateDistricts.put("Nagaland", new String[]{"Kohima", "Dimapur"});
        stateDistricts.put("Odisha", new String[]{"Bhubaneswar", "Cuttack", "Rourkela", "Sundargarh", "Khurda", "Puri"});
        stateDistricts.put("Punjab", new String[]{"Ludhiana", "Amritsar", "Jalandhar"});
        stateDistricts.put("Rajasthan", new String[]{"Jaipur", "Jodhpur", "Udaipur"});
        stateDistricts.put("Sikkim", new String[]{"Gangtok"});
        stateDistricts.put("Tamil Nadu", new String[]{"Chennai", "Coimbatore", "Madurai"});
        stateDistricts.put("Telangana", new String[]{"Hyderabad", "Warangal"});
        stateDistricts.put("Tripura", new String[]{"Agartala"});
        stateDistricts.put("Uttar Pradesh", new String[]{"Lucknow", "Kanpur", "Noida", "Varanasi"});
        stateDistricts.put("Uttarakhand", new String[]{"Dehradun", "Haridwar"});
        stateDistricts.put("West Bengal", new String[]{"Kolkata", "Howrah", "Darjeeling"});
        stateDistricts.put("Andaman and Nicobar Islands", new String[]{"South Andaman"});
        stateDistricts.put("Chandigarh", new String[]{"Chandigarh"});
        stateDistricts.put("Dadra and Nagar Haveli and Daman and Diu", new String[]{"Daman"});
        stateDistricts.put("Lakshadweep", new String[]{"Kavaratti"});
        stateDistricts.put("Delhi", new String[]{"New Delhi", "West Delhi"});
        stateDistricts.put("Puducherry", new String[]{"Puducherry"});
        stateDistricts.put("Ladakh", new String[]{"Leh"});
        stateDistricts.put("Jammu and Kashmir", new String[]{"Srinagar", "Jammu"});
        return stateDistricts;
    }
}
