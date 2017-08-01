# encoding: utf-8
require "logstash/outputs/base"
require "logstash/namespace"

require 'java'
require 'slf4j-api-1.7.12.jar'
require 'slf4j-log4j12-1.7.12.jar'
require 'log4j-1.2.17.jar'
require 'httpcore-4.4.1.jar'
require 'commons-codec-1.9.jar'
require 'commons-logging-1.2.jar'
require 'httpclient-4.4.1.jar'
require 'libthrift-0.9.3.jar'
require 'scribe-client-0.0.1.jar'
java_import 'com.bt.scribe.MultiScribeClient'
java_import 'scribe.thrift.LogEntry'
java_import 'com.bt.scribe.SendResult'

class LogStash::Outputs::Scribe < LogStash::Outputs::Base
  config_name "scribe"
  
  concurrency :single
  
  # Parameters you can specify in the logstash configuration
  config :host, :validate => :string, :required => true
  config :port, :validate => :number, :required => true
  config :category, :validate => :string, :default => "default-category", :required => false
  config :categoryParamName, :validate => :string, :default => "category", :required => false
  config :messageParamName, :validate => :string, :default => "message", :required => false
  config :maxSendOnce, :validate => :number, :default => 125, :required => false
  config :maxRetryTimes, :validate => :number, :default => 2, :require => false
  
  @scribeConnector = nil

  public
  def register
	@scribeConnector = MultiScribeClient::MultiScribeClientBuilder.new(@host, @port).maxMsgCountSendOnce(@maxSendOnce).maxRetryTimes(@maxRetryTimes).build
  end # def register

  public
  def multi_receive(events)
    logs = java.util.ArrayList.new(events.length)
	events.each do |event|
	  thisCategory = @category
	  msgCategory = event.get(@categoryParamName)
	  content = event.get(@messageParamName)
	  if content == nil
	    content = event.get('message')
	  end
	  if msgCategory != nil
	    thisCategory = msgCategory.to_s
	  end
	  
	  logs.add(LogEntry.new(thisCategory, content))
	end
	sendResult = @scribeConnector.sendMessages(logs)
	if sendResult.getExMsgs != nil
	  sendResult.getExMsgs.each do |ex|
	    @logger.error("#{ex.getErrorMsg} #{ex.getThrowable}")
	  end
	end
	if sendResult.getFailLogs != nil
	  sendResult.getFailLogs.each do |log|
	    @logger.info("#{log.getCategory} #{log.getMessage}")
	  end
	end
  end # def multi_receive
  
  public 
  def close
    @scribeConnector.close if @scribeConnector
  end # def close
end # class LogStash::Outputs::Scirbe
