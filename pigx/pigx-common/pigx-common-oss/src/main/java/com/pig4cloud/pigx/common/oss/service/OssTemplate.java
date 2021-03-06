/*
 *    Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of the pig4cloud.com developer nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * Author: lengleng (wangiegie@gmail.com)
 */

package com.pig4cloud.pigx.common.oss.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.util.IOUtils;
import com.pig4cloud.pigx.common.oss.OssProperties;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.InitializingBean;

/**
 * aws-s3 ?????????????????? ??????????????????s3??????????????????: {?????????OSS????????????COS???????????????????????????minio ???}
 *
 * @author lengleng
 * @author 858695266
 * @date 2020/5/23 6:36 ??????
 * @since 1.0
 */
@RequiredArgsConstructor
public class OssTemplate implements InitializingBean {

	private final OssProperties ossProperties;

	private AmazonS3 amazonS3;

	/**
	 * ??????bucket
	 * @param bucketName bucket??????
	 */
	@SneakyThrows
	public void createBucket(String bucketName) {
		if (!amazonS3.doesBucketExistV2(bucketName)) {
			amazonS3.createBucket((bucketName));
		}
	}

	/**
	 * ????????????bucket
	 * <p>
	 *
	 * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/s3-2006-03-01/ListBuckets">AWS
	 * API Documentation</a>
	 */
	@SneakyThrows
	public List<Bucket> getAllBuckets() {
		return amazonS3.listBuckets();
	}

	/**
	 * @param bucketName bucket??????
	 * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/s3-2006-03-01/ListBuckets">AWS
	 * API Documentation</a>
	 */
	@SneakyThrows
	public Optional<Bucket> getBucket(String bucketName) {
		return amazonS3.listBuckets().stream().filter(b -> b.getName().equals(bucketName)).findFirst();
	}

	/**
	 * @param bucketName bucket??????
	 * @see <a href=
	 * "http://docs.aws.amazon.com/goto/WebAPI/s3-2006-03-01/DeleteBucket">AWS API
	 * Documentation</a>
	 */
	@SneakyThrows
	public void removeBucket(String bucketName) {
		amazonS3.deleteBucket(bucketName);
	}

	/**
	 * ??????????????????????????????
	 * @param bucketName bucket??????
	 * @param prefix ??????
	 * @param recursive ??????????????????
	 * @return S3ObjectSummary ??????
	 * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/s3-2006-03-01/ListObjects">AWS
	 * API Documentation</a>
	 */
	@SneakyThrows
	public List<S3ObjectSummary> getAllObjectsByPrefix(String bucketName, String prefix, boolean recursive) {
		ObjectListing objectListing = amazonS3.listObjects(bucketName, prefix);
		return new ArrayList<>(objectListing.getObjectSummaries());
	}

	/**
	 * ??????????????????
	 * @param bucketName bucket??????
	 * @param objectName ????????????
	 * @param expires ???????????? <=7
	 * @return url
	 * @see AmazonS3#generatePresignedUrl(String bucketName, String key, Date expiration)
	 */
	@SneakyThrows
	public String getObjectURL(String bucketName, String objectName, Integer expires) {
		Date date = new Date();
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		calendar.add(Calendar.DAY_OF_MONTH, expires);
		URL url = amazonS3.generatePresignedUrl(bucketName, objectName, calendar.getTime());
		return url.toString();
	}

	/**
	 * ????????????
	 * @param bucketName bucket??????
	 * @param objectName ????????????
	 * @return ????????????
	 * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/s3-2006-03-01/GetObject">AWS
	 * API Documentation</a>
	 */
	@SneakyThrows
	public S3Object getObject(String bucketName, String objectName) {
		return amazonS3.getObject(bucketName, objectName);
	}

	/**
	 * ????????????
	 * @param bucketName bucket??????
	 * @param objectName ????????????
	 * @param stream ?????????
	 * @throws Exception
	 */
	public void putObject(String bucketName, String objectName, InputStream stream) throws Exception {
		putObject(bucketName, objectName, stream, (long) stream.available(), "application/octet-stream");
	}

	/**
	 * ????????????
	 * @param bucketName bucket??????
	 * @param objectName ????????????
	 * @param stream ?????????
	 * @param size ??????
	 * @param contextType ??????
	 * @throws Exception
	 * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/s3-2006-03-01/PutObject">AWS
	 * API Documentation</a>
	 */
	public PutObjectResult putObject(String bucketName, String objectName, InputStream stream, long size,
			String contextType) throws Exception {
		// String fileName = getFileName(objectName);
		byte[] bytes = IOUtils.toByteArray(stream);
		ObjectMetadata objectMetadata = new ObjectMetadata();
		objectMetadata.setContentLength(size);
		objectMetadata.setContentType(contextType);
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
		// ??????
		return amazonS3.putObject(bucketName, objectName, byteArrayInputStream, objectMetadata);

	}

	/**
	 * ??????????????????
	 * @param bucketName bucket??????
	 * @param objectName ????????????
	 * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/s3-2006-03-01/GetObject">AWS
	 * API Documentation</a>
	 */
	public S3Object getObjectInfo(String bucketName, String objectName) throws Exception {
		@Cleanup
		S3Object object = amazonS3.getObject(bucketName, objectName);
		return object;
	}

	/**
	 * ????????????
	 * @param bucketName bucket??????
	 * @param objectName ????????????
	 * @throws Exception
	 * @see <a href=
	 * "http://docs.aws.amazon.com/goto/WebAPI/s3-2006-03-01/DeleteObject">AWS API
	 * Documentation</a>
	 */
	public void removeObject(String bucketName, String objectName) throws Exception {
		amazonS3.deleteObject(bucketName, objectName);
	}

	@Override
	public void afterPropertiesSet() {
		ClientConfiguration clientConfiguration = new ClientConfiguration();
		clientConfiguration.setMaxConnections(ossProperties.getMaxConnections());

		AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(
				ossProperties.getEndpoint(), ossProperties.getRegion());
		AWSCredentials awsCredentials = new BasicAWSCredentials(ossProperties.getAccessKey(),
				ossProperties.getSecretKey());
		AWSCredentialsProvider awsCredentialsProvider = new AWSStaticCredentialsProvider(awsCredentials);
		this.amazonS3 = AmazonS3Client.builder().withEndpointConfiguration(endpointConfiguration)
				.withClientConfiguration(clientConfiguration).withCredentials(awsCredentialsProvider)
				.disableChunkedEncoding().withPathStyleAccessEnabled(ossProperties.getPathStyleAccess()).build();
	}

}
