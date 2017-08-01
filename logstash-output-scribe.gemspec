Gem::Specification.new do |s|
  s.name = 'logstash-output-scribe'
  s.version         = '1.0.1'
  s.licenses = ['Apache-2.0']
  s.summary = "This plugin help output to scribe-server"
  s.description     = "This gem is a Logstash plugin required to be installed on top of the Logstash core pipeline using $LS_HOME/bin/logstash-plugin install gemname. This gem is not a stand-alone program"
  s.authors = ["Loading"]
  s.email = "wyx.loading@aliyun.com"
  s.require_paths = ["lib","target"]
  s.platform = "java"

  # Files
  s.files = Dir['lib/**/*','target/*.jar','spec/**/*','vendor/**/*','*.gemspec','*.md','CONTRIBUTORS','Gemfile','LICENSE','NOTICE.TXT']
   # Tests
  s.test_files = s.files.grep(%r{^(test|spec|features)/})

  # Special flag to let us know this is actually a logstash plugin
  s.metadata = { "logstash_plugin" => "true", "logstash_group" => "output" }

  # Gem dependencies
  #
  s.add_runtime_dependency "logstash-core-plugin-api", ">= 1.60", "<= 2.99"
  s.add_runtime_dependency "logstash-codec-plain"
  s.add_development_dependency 'logstash-devutils'
end
