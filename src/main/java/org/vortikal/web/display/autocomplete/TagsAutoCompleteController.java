package org.vortikal.web.display.autocomplete;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.web.tags.Tag;

public class TagsAutoCompleteController extends AutoCompleteController {

    private VocabularyDataProvider<Tag> dataProvider;

    @Override
    protected List<Suggestion> getAutoCompleteSuggestions(String prefix, CompletionContext context) {

        List<Tag> completions = this.dataProvider.getCompletions(prefix, context);

        List<Suggestion> suggestions = new ArrayList<Suggestion>(completions.size());
        for (Tag tag: completions) {
            Suggestion suggestion = new Suggestion(1);
            suggestion.setField(0, tag.getText());
            suggestions.add(suggestion);
        }

        return suggestions;
    }

    @Required
    public void setDataProvider(VocabularyDataProvider<Tag> dataProvider) {
        this.dataProvider = dataProvider;
    }

}
