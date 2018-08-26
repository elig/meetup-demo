package org.demo

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class BundleBuilder {

    private boolean dryRun
    private def name
    private def description
    private def version
    private def queries = []
    private def mappings = []
    private def addedProps = []
    private def sourceArtifactoryId
    private boolean signImmediately = true
    private def releaseNotes
    protected static Logger logger = LoggerFactory.getLogger(this.getSimpleName())

    def sourceArtifactoryId(def sourceArtifactoryId) {
        this.sourceArtifactoryId = sourceArtifactoryId
        this
    }

    def signImmediately(boolean signImmediately) {
        this.signImmediately = signImmediately
        this
    }

    def dryRun(def dryRun) {
        this.dryRun = dryRun
        this
    }

    def name(def name) {
        this.name = name
        this
    }

    def description(def description) {
        this.description = description
        this
    }

    def version(def version) {
        this.version = version
        this
    }

    def addQuery(def aql) {
        def query = [
                aql: aql
        ]
        if (mappings) {
            query.put("mappings", mappings)
        }
        if (addedProps) {
            query.put("added_props", addedProps)
        }
        queries.add(query)
        this
    }

    def addProp(def key, def values = []) {
        def prop = [
                key   : key,
                values: values
        ]
        this.addedProps.add(prop)
        return this
    }

    def addMapping(def input, def output) {
        def mapping = [
                input : input,
                output: output
        ]
        mappings.add(mapping)
        this
    }

    def addReleaseNotes(def releaseNotes){
        this.releaseNotes = releaseNotes
        this
    }

    def build() {
        def bundle = [
                sign_immediately: signImmediately,
                dry_run         : dryRun,
                name            : name,
                version         : version,
                spec            : [
                        "queries"              : queries,
                        "source_artifactory_id": sourceArtifactoryId
                ]

        ]
        if (description) {
            bundle.put("description", description)
        }
        if(releaseNotes){
            bundle.put("release_notes", releaseNotes)
        }
        return bundle
    }


    static class ReleaseNotesBuilder {

        def syntax = null
        def content = null

        def addSyntax(Syntax syntax) {
            this.syntax = syntax.toString()
            this
        }
        def addContent(def content){
            this.content = content
            this
        }
        def build(){

            return [syntax: syntax, content: content]

        }

        static enum Syntax {
            PLAIN_TEXT("plain_text"),
            MARKDOWN("markdown"),
            ASCIDOC("ascidoc"),

            private final String text
            Syntax(String text) { this.text = text }

            String toString() {
                return text
            }

        }
    }

}