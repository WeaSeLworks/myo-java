
#Install java

include jdk7
 jdk7::install7{ 'jdk1.8.0_25':
  version              => "8u25" , 
  fullVersion          => "jdk1.8.0_25",
  javaHomes            => '/usr/java/',
  alternativesPriority => 18000, 
  x64                  => false,
  downloadDir          => "/install",
  urandomJavaFix       => false,
  sourcePath           => "puppet:///modules/jdk7/"
}


package { "fakeroot":
       ensure => "latest"
}
