/*
 * Copyright 2026 HM Revenue & Customs
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

package navigation

import config.FrontendAppConfig
import controllers.routes
import models.{FileUpload, FileUploadStatus, FileUploadValidationStatus, InvalidFileReason}
import play.api.mvc.Call

import javax.inject.{Inject, Singleton}

sealed trait FileProcessingDecision

object FileProcessingDecision {
  case object Processing extends FileProcessingDecision
  final case class Completed(destination: Call) extends FileProcessingDecision
  final case class Failed(destination: Call) extends FileProcessingDecision
}

@Singleton
class FileUploadResultNavigator @Inject() (appConfig: FrontendAppConfig) {

  import FileProcessingDecision.*

  private val genericFailureDestination: Call = routes.FileUploadErrorController.fileUploadFailed()

  def getFileProcessingOutcome(fileUpload: Option[FileUpload], reference: String): FileProcessingDecision =
    fileUpload match {
      case Some(upload) if FileUploadStatus.pending.contains(upload.status)  => Processing
      case Some(upload) if FileUploadStatus.terminal.contains(upload.status) =>
        Completed(destination(upload, reference))
      case _                                                                 => Failed(genericFailureDestination)
    }

  private def destination(fileUpload: FileUpload, reference: String): Call =
    fileUpload.status match {
      case FileUploadStatus.UpscanQuarantine if fileUpload.isPasswordProtected =>
        routes.FileUploadErrorController.filePasswordProtected()
      case FileUploadStatus.UpscanQuarantine                                   =>
        routes.FileUploadErrorController.fileContainsVirus()
      case FileUploadStatus.UpscanRejected                                     =>
        routes.FileUploadErrorController.invalidFileType()
      case FileUploadStatus.Duplicate                                          =>
        routes.FileUploadErrorController.duplicateFileUpload()
      case FileUploadStatus.ValidationSuccess                                  =>
        routes.UploadedReportFilesController.onPageLoad()
      case FileUploadStatus.ValidationFailure                                  =>
        validationFailureDestination(fileUpload, reference)
      case _                                                                   =>
        genericFailureDestination
    }

  private def validationFailureDestination(fileUpload: FileUpload, reference: String): Call =
    fileUpload.fileUploadDetails.flatMap(_.validation) match {
      case Some(validation) if validation.status == FileUploadValidationStatus.InvalidFile =>
        validation.invalidFileReason match {
          case Some(
                InvalidFileReason.InvalidHeader | InvalidFileReason.InvalidWorkbook | InvalidFileReason.InvalidFile
              ) =>
            routes.ProblemWithUploadedFileController.onPageLoad()
          case Some(InvalidFileReason.NoDataRows)          =>
            routes.FileUploadErrorController.emptyFileUploaded()
          case Some(InvalidFileReason.UnsupportedFileType) =>
            routes.FileUploadErrorController.invalidFileType()
          case _                                           =>
            genericFailureDestination
        }
      case Some(validation)
          if validation.status == FileUploadValidationStatus.ValidationFailed &&
            validation.validationErrors > appConfig.fileUploadMaxInlineErrors =>
        routes.FileFormattingErrorsController.onPageLoad()
      case Some(validation)
          if validation.status == FileUploadValidationStatus.ValidationFailed && validation.inlineErrors.nonEmpty =>
        routes.FileValidationErrorsController.onPageLoad(reference)
      case _                                                                               =>
        genericFailureDestination
    }
}
