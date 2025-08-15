/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package service

import models.{UploadDetails, UploadId, UploadStatus}
import org.bson.types.ObjectId
import repositories.UpscanRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UploadProgressTracker @Inject()(
                                       repository: UpscanRepository,
                                       
)(implicit ec:
  ExecutionContext
) {

  def requestUpload(uploadId: UploadId, fileReference: String,isaManagerReference:String): Future[Unit] =
    repository.insert(UploadDetails(ObjectId.get(), uploadId, fileReference, UploadStatus.InProgress,isaManagerReference))

  def registerUploadResult(fileReference: String, uploadStatus: UploadStatus)
                          (using hc: HeaderCarrier): Future[Unit] = {
    for
      _ <- repository.updateStatus(fileReference, uploadStatus)
    yield
      ()
  }

  def getUploadResult(id: UploadId): Future[Option[UploadStatus]] =
    repository
      .findByUploadId(id)
      .map(_.map(_.status))
      
  def getAllUploads(isaManagerRef:String): Future[Seq[UploadStatus]] = 
    repository
      .findByIsaManagerRef(isaManagerRef).map(_.map(_.status)) 
      
  def removeFile(isaManagerRef:String, name: String): Future[Unit] = repository.deleteFile(isaManagerRef,name)  
}