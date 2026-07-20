import sbt.Setting
import scoverage.ScoverageKeys

object CodeCoverageSettings {

  private val excludedPackages: Seq[String] = Seq(
    "<empty>",
    "Reverse.*",
    "uk.gov.hmrc.BuildInfo",
    "uk.gov.hmrc.disaregistration.config.AppConfig",
    "app.*",
    "prod.*",
    ".*Routes.*",
    "testOnly.*",
    "testOnlyDoNotUseInAppConf.*",
    "uk.gov.hmrc.disaregistration.Module",
    "uk.gov.hmrc.disaregistration.AppInitialiser",
    "uk.gov.hmrc.disaregistration.models.journeyData.FeesCommissionsAndIncentives",
    "uk.gov.hmrc.disaregistration.models.journeyData.OutsourcedAdministration"
  )

  val settings: Seq[Setting[?]] = Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}
