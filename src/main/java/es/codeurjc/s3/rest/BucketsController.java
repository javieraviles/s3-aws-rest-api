package es.codeurjc.s3.rest;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.PutObjectResult;

@RestController
@CrossOrigin(methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE })
@RequestMapping("/api/buckets")
public class BucketsController {

	@Autowired
	private AmazonS3 s3;

	@GetMapping("/")
	public Collection<Bucket> items() {
		return s3.listBuckets();
	}

	@GetMapping("/{bucketName}")
	public ResponseEntity<Bucket> getBucket(@PathVariable String bucketName) {

		final Bucket fetchedBucket = s3.listBuckets().stream().filter(bucket -> bucketName.equals(bucket.getName()))
				.findFirst().orElse(null);

		if (fetchedBucket != null) {
			return new ResponseEntity<>(fetchedBucket, HttpStatus.OK);
		} else {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}

	@PostMapping("/{bucketName}")
	public ResponseEntity<Bucket> createBucket(@PathVariable String bucketName) {

		if (!s3.doesBucketExistV2(bucketName)) {
			return new ResponseEntity<>(s3.createBucket(bucketName), HttpStatus.CREATED);
		} else {
			return new ResponseEntity<>(HttpStatus.CONFLICT);
		}
	}

	@PostMapping("/{bucketName}/uploadObject")
	public ResponseEntity<PutObjectResult> createObject(@PathVariable String bucketName,
			@RequestParam("file") MultipartFile multiPartFile) throws IllegalStateException, IOException {

		final String fileName = multiPartFile.getOriginalFilename();
		final File file = new File(System.getProperty("java.io.tmpdir") + "/" + fileName);
		multiPartFile.transferTo(file);

		final PutObjectResult result = s3.putObject(bucketName, fileName, file);

		return new ResponseEntity<>(result, HttpStatus.CREATED);

	}

	@DeleteMapping("/{bucketName}")
	public ResponseEntity<Void> deleteBucket(@PathVariable String bucketName) {

		if (s3.doesBucketExistV2(bucketName)) {
			if (s3.listObjects(bucketName).getObjectSummaries().isEmpty()) {
				s3.deleteBucket(bucketName);
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			}
			return new ResponseEntity<>(HttpStatus.CONFLICT);
		}
		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	}

	@DeleteMapping("/{bucketName}/{objectName}")
	public ResponseEntity<Void> deleteBucket(@PathVariable String bucketName, @PathVariable String objectName) {

		if (s3.doesObjectExist(bucketName, objectName)) {
			s3.deleteObject(bucketName, objectName);
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	}

}
