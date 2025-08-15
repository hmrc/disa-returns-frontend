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

package repositories

import com.google.inject.Inject
import models.{UploadDetails, UploadId, UploadStatus}
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.model.{Filters, FindOneAndUpdateOptions, IndexModel, IndexOptions, Indexes}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import scala.concurrent.{ExecutionContext, Future}

class UpscanRepository @Inject() (mc: MongoComponent)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[UploadDetails](
      mongoComponent = mc,
      collectionName = "upscanRepository",
      domainFormat = UploadDetails.format,
      indexes = Seq(
        IndexModel(Indexes.ascending("uploadId"), IndexOptions().unique(true)),
        IndexModel(Indexes.ascending("reference"), IndexOptions().unique(true))
      )
    ) {

  def insert(details: UploadDetails): Future[Unit]                      =
    collection
      .insertOne(details)
      .toFuture()
      .map(_ => ())
  def findByUploadId(uploadId: UploadId): Future[Option[UploadDetails]] =
    collection.find(equal("uploadId", Codecs.toBson(uploadId.value))).headOption()

  def findByIsaManagerRef(isaManagerReference: String): Future[Seq[UploadDetails]] =
    collection.find(equal("isaManagerReference", isaManagerReference)).toFuture()

  def updateStatus(reference: String, newStatus: UploadStatus): Future[UploadStatus] =
    collection
      .findOneAndUpdate(
        filter = equal("reference", Codecs.toBson(reference)),
        update = set("status", Codecs.toBson(newStatus)),
        options = FindOneAndUpdateOptions().upsert(true)
      )
      .toFuture()
      .map(_.status)
      
  def deleteFile(isaManagerReference:String, name:String): Future[Unit] = {
    val filter = Filters.and(
      equal("isaManagerReference", isaManagerReference),
      equal("status.name", name)
    )
    collection.deleteOne(filter).toFuture().map(_=> ())
  }
}
