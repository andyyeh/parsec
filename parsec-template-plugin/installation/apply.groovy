buildscript {
  repositories {
    jcenter()
    maven {
      url 'http://dl.bintray.com/cjstehno/public'
    }
  }
  dependencies {
    classpath "com.yahoo.parsec:parsec-template-plugin:1.0.5"
  }
}

allprojects {
    apply plugin: com.yahoo.parsec.template.ParsecTemplatePlugin
}
