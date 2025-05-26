package xyz.yygqzzk.trigger.http;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.core.io.PathResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import xyz.yygqzzk.api.IRAGService;
import xyz.yygqzzk.api.response.Response;
import xyz.yygqzzk.types.enums.ResponseCode;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
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
    @RequestMapping(value = "/query_rag_tag_list", method = RequestMethod.GET)
    public Response<List<String>> queryRagTagList() {
        RList<String> elements = redissonClient.getList("rag_tag");
        return Response.<List<String>>builder()
                .data(elements)
                .code("0000")
                .info("成功")
                .build();
    }

    @Override
    @RequestMapping(value = "/file/upload", method = RequestMethod.POST)
    public Response<String> uploadFile(@RequestParam String ragTag, @RequestParam("file") List<MultipartFile> files) {
        log.info("上传知识库开始 {}", ragTag);

        for (MultipartFile file : files) {
            TikaDocumentReader reader = new TikaDocumentReader(file.getResource());

            List<Document> documents = tokenTextSplitter.apply(reader.get());
            documents.forEach(doc -> {
                doc.getMetadata().put("knowledge", ragTag);
            });
            pgVectorStore.accept(documents);

            log.info("{} 上传完成", file.getName());
        }

        // redis 中存储各个知识库的标签 ragtag
        RList<Object> elements = redissonClient.getList("rag_tag");
        if(!elements.contains(ragTag)){
            elements.add(ragTag);
        }

        log.info("知识库上传完成 {}", ragTag);
        return Response.<String>builder()
                .code("0000")
                .info("上传成功")
                .build();
    }

    @Override
    @RequestMapping(value = "analyze_git_repository", method = RequestMethod.POST)
    public Response<String> analyzeGitRepository(@RequestParam String repoUrl, @RequestParam String userName, @RequestParam String token) throws Exception {

        String localPath = "./git-cloned-repo";
        String repoProjectName = extractProjectName(repoUrl);
        log.info("克隆路径：{}", new File(localPath).getAbsolutePath());

        FileUtils.deleteDirectory(new File(localPath));

        Git git = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(new File(localPath))
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, token))
                .call();

        Files.walkFileTree(Paths.get(localPath), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                log.info("{} 遍历解析路径，上传知识库:{}", repoProjectName, file.getFileName());
                try {
                    TikaDocumentReader reader = new TikaDocumentReader(new PathResource(file));
                    List<Document> documents = reader.get();
                    List<Document> documentSplitterList = tokenTextSplitter.apply(documents);

                    documents.forEach(doc -> doc.getMetadata().put("knowledge", repoProjectName));

                    documentSplitterList.forEach(doc -> doc.getMetadata().put("knowledge", repoProjectName));

                    pgVectorStore.accept(documentSplitterList);
                } catch (Exception e) {
                    log.error("遍历解析路径，上传知识库失败:{}", file.getFileName());
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                log.info("Failed to access file: {} - {}", file.toString(), exc.getMessage());
                return FileVisitResult.CONTINUE;
            }
        });

        try {
            FileUtils.deleteDirectory(new File(localPath));

        } catch (IOException e) {
            log.info("文件删除失败: {}", e.getMessage());
        }

        RList<String> elements = redissonClient.getList("ragTag");
        if (!elements.contains(repoProjectName)) {
            elements.add(repoProjectName);
        }

        git.close();

        log.info("遍历解析路径，上传完成:{}", repoUrl);

        return Response.<String>builder().code("0000").info("调用成功").build();
    }

    private String extractProjectName(String repoUrl) {
        String[] parts = repoUrl.split("/");
        String projectNameWithGit = parts[parts.length - 1];
        return projectNameWithGit.replace(".git", "");
    }
}




