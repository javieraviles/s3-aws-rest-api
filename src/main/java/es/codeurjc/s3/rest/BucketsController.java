package es.codeurjc.s3.rest;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

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
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CopyObjectResult;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3ObjectSummary;

@RestController
@CrossOrigin(methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE })
@RequestMapping("/api/buckets")
public class BucketsController {

	@Autowired
	private AmazonS3 s3;

	@GetMapping("/")
	public Collection<Bucket> getBuckets() {
		return s3.listBuckets();
	}

	@GetMapping("/{bucketName}/objects")
	public ResponseEntity<List<S3ObjectSummary>> getBucket(@PathVariable String bucketName) {

		if (s3.doesBucketExistV2(bucketName)) {
			final List<S3ObjectSummary> result = s3.listObjects(bucketName).getObjectSummaries();
			return new ResponseEntity<>(result, HttpStatus.OK);
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
			@RequestParam("file") MultipartFile multiPartFile, @RequestParam("isPublic") Boolean isPublic)
			throws IllegalStateException, IOException {

		if (s3.doesBucketExistV2(bucketName)) {
			final String fileName = multiPartFile.getOriginalFilename();
			final File file = new File(System.getProperty("java.io.tmpdir"), fileName);
			multiPartFile.transferTo(file);

			final PutObjectRequest objectRequest = new PutObjectRequest(bucketName, fileName, file);
			if (isPublic) {
				objectRequest.setCannedAcl(CannedAccessControlList.PublicRead);
			}
			final PutObjectResult result = s3.putObject(objectRequest);

			return new ResponseEntity<>(result, HttpStatus.CREATED);
		} else {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

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

	@PostMapping("/{sourceBucketName}/{objectName}/copy")
	public ResponseEntity<CopyObjectResult> copyObject(@PathVariable String sourceBucketName,
			@PathVariable String objectName, @RequestParam("destinationBucketName") String destinationBucketName) {
		if (s3.doesObjectExist(sourceBucketName, objectName)) {
			final CopyObjectResult result = s3.copyObject(sourceBucketName, objectName, destinationBucketName,
					objectName);
			return new ResponseEntity<>(result, HttpStatus.CREATED);
		}
		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	}

}
