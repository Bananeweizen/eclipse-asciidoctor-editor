/*
 * Copyright 2018 Albert Tregnaghi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 */
package de.jcup.asciidoctoreditor.provider;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

import org.asciidoctor.Attributes;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;

import de.jcup.asciidoctoreditor.asciidoc.AsciiDoctorBackendType;

public class AsciiDoctorOptionsProvider extends AbstractAsciiDoctorProvider {

    AsciiDoctorOptionsProvider(AsciiDoctorProviderContext context) {
        super(context);
    }

    public Map<String, Object> createDefaultOptions(AsciiDoctorBackendType backend) {
        /* @formatter:off*/
		Attributes attrs;
		Path outputFolder = getContext().getOutputFolder();
		if (outputFolder==null){
			throw new IllegalStateException("output folder not defined");
		}
		
		AttributesBuilder attrBuilder = AttributesBuilder.
				attributes().
					showTitle(true).
					noFooter(getContext().isNoFooter()).
					sourceHighlighter("coderay").
					attribute("eclipse-editor-basedir",getContext().getBaseDir().getAbsolutePath()).
				    attribute("icons", "font").
					attribute("source-highlighter","coderay").
					attribute("coderay-css", "style").
					attribute("env", "eclipse").
					attribute("env-eclipse");
		 /* @formatter:on*/
        Map<String, Object> cachedAttributes = getContext().getAttributesProvider().getCachedAttributes();
        for (String key : cachedAttributes.keySet()) {
            Object value = cachedAttributes.get(key);
            if (value == null || value.toString().isEmpty()) {
                continue;
            }
            if ("toc".equals(key)) {
                // currently we always remove the TOC (we do show the TOC
                // only by the internal boolean flag
                // also the TOC is not correctly positioned - (always on top
                // instead of being at left side)
                continue;
            }
            attrBuilder.attribute(key, value);
        }
        if (getContext().isTOCVisible()) {
            attrBuilder.attribute("toc", "left");
            if (getContext().tocLevels > 0) {
                attrBuilder.attribute("toclevels", "" + getContext().tocLevels);
            }
        }
        ImageHandlingMode imageHandlingMode = getContext().getImageHandlingMode();
        if (imageHandlingMode == ImageHandlingMode.IMAGESDIR_FROM_PREVIEW_DIRECTORY) {
            /*
             * when this is enabled, we do:<br> 1. Force images copied and available at
             * target image directory, so images in html preview in same origin 2. Image
             * output directory (e.g. for diagram generation) targets also this image output
             * directory 3. Images directory attribute is set to target directory, so
             * diagram output and existing images are in same folder
             */
            getContext().getImageProvider().ensureImages();
            attrBuilder.attribute("imagesoutdir", createAbsolutePath(getContext().targetImagesDir.toPath()));
            attrBuilder.imagesDir(getContext().targetImagesDir.getAbsolutePath());
        } else if (imageHandlingMode == ImageHandlingMode.RELATIVE_PATHES) {
            /*
             * for relative pathes - without attribute ':imagedir:' set in asciidoc files
             * this seems to be necessary
             */
            attrBuilder.imagesDir(getContext().getBaseDir().getAbsolutePath());
        } else if (imageHandlingMode == ImageHandlingMode.STORE_DIAGRAM_FILES_LOCAL) {
            String imagesoutpath = null;
            File editorFileOrNull = getContext().getEditorFileOrNull();
            if (editorFileOrNull != null) {
                imagesoutpath = createAbsolutePath(editorFileOrNull.getParentFile().toPath());
                attrBuilder.imagesDir(imagesoutpath);
                attrBuilder.attribute("imagesoutdir", imagesoutpath);
            }
        } else {
            /* other mode(s) currently not implemented */
        }

        attrs = attrBuilder.get();
        attrs.setAttribute("outdir", createAbsolutePath(outputFolder));

        File destionationFolder = outputFolder.toFile();

        OptionsBuilder opts = OptionsBuilder.options().toDir(destionationFolder).safe(SafeMode.UNSAFE).backend(backend.getBackendString()).headerFooter(getContext().isTOCVisible()).

                attributes(attrs).option("sourcemap", "true").baseDir(getContext().getBaseDir());
        /* @formatter:on*/
        return opts.asMap();
    }

    protected String createAbsolutePath(Path path) {
        return path.toAbsolutePath().normalize().toString();
    }

    public void reset() {
        /* nothing to do */
    }

}
