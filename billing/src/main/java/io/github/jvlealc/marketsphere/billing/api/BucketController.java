package io.github.jvlealc.marketsphere.billing.api;

import io.github.jvlealc.marketsphere.billing.bucket.BucketFile;
import io.github.jvlealc.marketsphere.billing.bucket.BucketService;
import io.github.jvlealc.marketsphere.billing.bucket.exception.StorageAccessException;
import io.github.jvlealc.marketsphere.billing.translator.MessageTranslator;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

@RestController
@RequestMapping("/bucket")
public class BucketController {

    private final BucketService service;
    private final MessageTranslator translator;

    public BucketController(BucketService service, MessageTranslator translator) {
        this.service = service;
        this.translator = translator;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadFile(@RequestParam(value = "file") MultipartFile multipartFile) {
        try (InputStream inputStream = multipartFile.getInputStream()) {

            String contentType = multipartFile.getContentType();
            MediaType mediaType;
            if (contentType == null || contentType.isBlank()) {
                // Define o tipo como arquivo binário genérico se o tipo não for informado
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            } else {
                mediaType = MediaType.parseMediaType(contentType);
            }

            service.upload(new BucketFile(
                    multipartFile.getOriginalFilename(),
                    inputStream, mediaType,
                    multipartFile.getSize()
            ));

            URI location = this.generateHeaderLocation(multipartFile.getOriginalFilename());
            String successMessage = translator.translate("file.upload.successful");
            return ResponseEntity.created(location).body(successMessage);

        } catch (IOException e) {
            throw new StorageAccessException("Could not read input stream from uploaded multipartFile", e);
        }
    }

    @GetMapping(value = "/{fileName:.+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getFileUrl(@PathVariable String fileName) {
        String fileUrl = service.generatePresignedUrl(fileName);
        return ResponseEntity.ok(fileUrl);
    }

    private URI generateHeaderLocation(String fileName) {
        return ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{filename}")
                .buildAndExpand(fileName)
                .toUri();
    }
}
