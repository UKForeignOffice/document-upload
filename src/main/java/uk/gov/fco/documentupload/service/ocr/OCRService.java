package uk.gov.fco.documentupload.service.ocr;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.model.*;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.*;
import uk.gov.fco.documentupload.service.storage.FileUpload;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.*;

import software.amazon.awssdk.services.rekognition.RekognitionClient;

@Service
@Slf4j
public class OCRService {
    private boolean enabled;

    private int sharpnessThreshold;

    private RekognitionClient rekognition;

    private TextractClient textract;

    @Autowired
    public OCRService(@Value("${ocr.enabled}") @NotNull boolean enabled, @Value("${ocr.sharpness.threshold}") int sharpnessThreshold){
        this.enabled = enabled;
        this.sharpnessThreshold = sharpnessThreshold;
        rekognition = RekognitionClient.builder().region(Region.EU_WEST_2).credentialsProvider(EnvironmentVariableCredentialsProvider.create()).build();
        textract = TextractClient.builder().region(Region.EU_WEST_2).credentialsProvider(EnvironmentVariableCredentialsProvider.create()).build();
    }

    public boolean passesQualityCheck(FileUpload upload) throws IOException {
        if(enabled){
            log.info("Starting image quality check");
            SdkBytes bytes = SdkBytes.fromInputStream(upload.getInputStream());
            Image image = Image.builder().bytes(bytes).build();
            DetectLabelsRequest request = DetectLabelsRequest.builder().image(image).featuresWithStrings("IMAGE_PROPERTIES").build();
            DetectLabelsImageProperties results = rekognition.detectLabels(request).imageProperties();
            return results.quality().sharpness() >= sharpnessThreshold;
        }
        return true;
    }

    public String extractData(FileUpload upload) throws IOException {
        if(enabled){
            log.info("Starting data extraction");
            try{
                SdkBytes bytes = SdkBytes.fromInputStream(upload.getInputStream());
                Document doc = Document.builder().bytes(bytes).build();

                List<FeatureType> featureTypes = new ArrayList<>();
                featureTypes.add(FeatureType.QUERIES);

                Query firstNameQuery = Query.builder().alias("firstName").text("Retrieve the subject's first name").build();
                Query lastNameQuery = Query.builder().alias("lastName").text("Retrieve the subject's surname").build();
                Query dobQuery = Query.builder().alias("dob").text("Retrieve the user's date of birth").build();

                Collection<Query> queries = new ArrayList<>(Arrays.asList(firstNameQuery, lastNameQuery, dobQuery));

                QueriesConfig queryConfig = QueriesConfig.builder().queries(queries).build();

                AnalyzeDocumentRequest request = AnalyzeDocumentRequest.builder().featureTypes(featureTypes).document(doc).queriesConfig(queryConfig).build();

                AnalyzeDocumentResponse response = textract.analyzeDocument(request);

                List<Block> blocks = response.blocks();
                List<Block> queryBlocks = this.getBlocksByType(blocks, BlockType.QUERY);
                List<Block> queryResultBlocks = this.getBlocksByType(blocks, BlockType.QUERY_RESULT);

                HashMap<String, String> results = mapQueriesToAnswers(queryBlocks, queryResultBlocks);

                return new ObjectMapper().writeValueAsString(results);

            }catch(TextractException e) {
                log.error(e.getMessage());
            }
        }
        return "{}";
    }

    private List<Block> getBlocksByType(List<Block> blocks, BlockType type) {
        List<Block> queryBlocks = new ArrayList<>();
        for(Block block : blocks){
            if(block.blockType() == type){
                queryBlocks.add(block);
            }
        }
        return queryBlocks;
    }

    private String getAnswerId(List<Relationship> relationships){
        String id = "";
        for(Relationship relationship: relationships){
            if(relationship.type() == RelationshipType.ANSWER){
                id = relationship.ids().get(0);
                break;
            }
        }
        return id;
    }

    private String getAnswer(List<Block> blocks, String blockId){
        String answer = "";
        for(Block block : blocks){
            if(Objects.equals(block.id(), blockId)){
                answer = block.text();
                break;
            }
        }
        return answer;
    }

    private HashMap<String, String> mapQueriesToAnswers(List<Block> queryBlocks, List<Block> queryResultBlocks) {
        HashMap<String, String> hashMap = new HashMap<>();
        for(Block block: queryBlocks){
            String answerId = getAnswerId(block.relationships());
            String answer = getAnswer(queryResultBlocks, answerId);
            if(!Objects.equals(answer, "")){
                hashMap.put(block.query().alias(), answer);
            }
        }
        return hashMap;
    }
}
