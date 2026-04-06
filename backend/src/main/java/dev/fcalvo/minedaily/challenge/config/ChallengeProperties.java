package dev.fcalvo.minedaily.challenge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "minedaily.challenge")
public class ChallengeProperties {

	private String timezone = "America/Argentina/Cordoba";
	private int rolloverHour = 21;
	private String rolloverCron = "0 0 21 * * *";
	private int rows = 10;
	private int cols = 10;
	private int mineCount = 18;
	private String generatorVersion = "v1";
	private String seedNamespace = "minedaily-local-v1";

	public String getTimezone() {
		return timezone;
	}

	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	public int getRolloverHour() {
		return rolloverHour;
	}

	public void setRolloverHour(int rolloverHour) {
		this.rolloverHour = rolloverHour;
	}

	public String getRolloverCron() {
		return rolloverCron;
	}

	public void setRolloverCron(String rolloverCron) {
		this.rolloverCron = rolloverCron;
	}

	public int getRows() {
		return rows;
	}

	public void setRows(int rows) {
		this.rows = rows;
	}

	public int getCols() {
		return cols;
	}

	public void setCols(int cols) {
		this.cols = cols;
	}

	public int getMineCount() {
		return mineCount;
	}

	public void setMineCount(int mineCount) {
		this.mineCount = mineCount;
	}

	public String getGeneratorVersion() {
		return generatorVersion;
	}

	public void setGeneratorVersion(String generatorVersion) {
		this.generatorVersion = generatorVersion;
	}

	public String getSeedNamespace() {
		return seedNamespace;
	}

	public void setSeedNamespace(String seedNamespace) {
		this.seedNamespace = seedNamespace;
	}

}
