package com.example.scanner.demo

import com.example.scanner.demo.SpringFilteringProofDemo
import spock.lang.Specification
import spock.lang.Subject

/**
 * Test that runs the Spring filtering proof demo.
 */
class SpringFilteringProofDemoSpec extends Specification {

    @Subject
    SpringFilteringProofDemo demo

    def setup() {
        demo = new SpringFilteringProofDemo()
    }

    def "should run Spring filtering proof demo successfully"() {
        when: "running the demo"
        demo.demonstrateSpringFiltering()
        
        then: "should complete without exceptions"
        noExceptionThrown()
    }
}
