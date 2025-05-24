package xyz.yygqzzk.trigger.http;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import xyz.yygqzzk.api.IRAGService;
import xyz.yygqzzk.api.response.Response;
import xyz.yygqzzk.types.enums.ResponseCode;

import java.util.List;

/**
 * @author zzk
 * @version 1.0
 * @description TODO
 * @since 2025/5/24
 */

@RestController
@CrossOrigin("*")
@RequestMapping("/api/v1/rag")
@Slf4j
public class RAGController implements IRAGService {


    @Resource
    private OllamaChatClient ollamaChatClient;

    @Resource
    private TokenTextSplitter tokenTextSplitter;

    @Resource
    private PgVectorStore pgVectorStore;

    @Resource
    private RedissonClient redissonClient;


    @Override
    @RequestMapping(value = "/rag_tag_list", method = RequestMethod.GET)
    public Response<List<String>> queryRagTagList() {
        RList<String> elements = redissonClient.getList("rag_tag");
        return Response.<List<String>>builder()
                .data(elements)
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .build();
    }

    @Override
    @RequestMapping(value = "/upload_file", method = RequestMethod.POST)
    public Response<String> uploadFile(@RequestParam String ragTag, @RequestParam("file") List<MultipartFile> files) {
        log.info("上传知识库开始 {}", ragTag);

        for (MultipartFile file : files) {
            TikaDocumentReader reader = new TikaDocumentReader(file.getResource());

            List<Document> documents = tokenTextSplitter.apply(reader.get());
            documents.forEach(doc -> {
                doc.getMetadata().put("knowledge", ragTag);
            });
            pgVectorStore.accept(documents);

            RList<Object> elements = redissonClient.getList("rag_tag");
            if(!elements.contains(ragTag)){
                elements.add(ragTag);
            }
            log.info("{} 上传完成", file.getName());

        }

        log.info("知识库上传完成 {}", ragTag);
        return Response.<String>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .build();
    }
}




