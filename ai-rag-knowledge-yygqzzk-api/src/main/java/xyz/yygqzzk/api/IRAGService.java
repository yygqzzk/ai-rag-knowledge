package xyz.yygqzzk.api;

import org.springframework.web.multipart.MultipartFile;
import xyz.yygqzzk.api.response.Response;

import java.util.List;

/**
 * @author zzk
 * @version 1.0
 * @description TODO
 * @since 2025/5/24
 */
public interface IRAGService {

    Response<List<String>> queryRagTagList();

    Response<String> uploadFile(String ragTag, List<MultipartFile> files);

}
