package org.vortikal.web.display.autocomplete;

import java.util.List;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.web.tags.Tag;

public class TagsAutoCompleteController extends AutoCompleteController {

    private VocabularyDataProvider<Tag> dataProvider;

    @Override
    protected String getAutoCompleteSuggestions(String prefix, Path contextUri,
            String securityToken) {

        List<Tag> completions = this.dataProvider.getPrefixCompletions(prefix, null,
                securityToken);

        StringBuilder result = new StringBuilder();
        for (Tag tag : completions) {
            result.append(tag.getText() + SUGGESTION_DELIMITER);
        }
        return result.toString();
    }

    @Required
    public void setDataProvider(VocabularyDataProvider<Tag> dataProvider) {
        this.dataProvider = dataProvider;
    }

}
