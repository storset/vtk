package org.vortikal.repository.content;

import org.vortikal.repository.store.AudioMetadata;

public class ExternalProgramAudioContentFactory extends AbstractExternalProgramContentFactory implements ContentFactory{
    @Override
    public Class<?>[] getRepresentationClasses() {
        return new Class<?>[] { AudioMetadata.class };
    }
}
