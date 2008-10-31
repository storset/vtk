package org.vortikal.integration.webtests.admin;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.jwebunit.html.Cell;
import net.sourceforge.jwebunit.html.Row;
import net.sourceforge.jwebunit.html.Table;

import org.vortikal.integration.webtests.BaseAuthenticatedWebTest;

public class CollectionListingTest extends BaseAuthenticatedWebTest {
    
    private enum SORT_ORDER {
        ASC,
        DESC
    }
    
    private final String directoryListingTableId = "vrtx-directoryListing";

    public void testSortCollections() {
                
        Table directoryListing = getTable(directoryListingTableId);
        assertNotNull(directoryListing);
        
        // Listing is by default sorted asc on name, click it once to sort it desc 
        // so that it matches the other properties once the sortingassertions begin
        clickLink("name");
        
        // Sort by name
        assertSortCollectionListing("name", 0); 
        
        // Sort by owner
        assertSortCollectionListing("owner", 2);
        
        // Sort by last modified
        assertSortCollectionListing("last-modified", 5);
    }
    
    public void testPreview() {
        clickLink("previewCollectionListingService");
        assertElementPresent("previewIframe");
    }

    private void assertSortCollectionListing(String sortOrderLinkId, int cellIndex) {
        clickLink(sortOrderLinkId);
        assertSortOrder(getTable(directoryListingTableId), cellIndex, SORT_ORDER.ASC);
        clickLink(sortOrderLinkId);
        assertSortOrder(getTable(directoryListingTableId), cellIndex, SORT_ORDER.DESC);
    }

    private void assertSortOrder(Table directoryListing, int cellIndex, SORT_ORDER sortOrder) {
        List<Row> tableRows = directoryListing.getRows();
        
        // Remove the header row
        tableRows.remove(0);
        
        assertTrue("No resources in table directoryListing", tableRows.size() > 0);
        
        List<String> resourceList = new ArrayList<String>();
        for (Row row : tableRows) {
            List<Cell> rowCells = row.getCells();
            resourceList.add(rowCells.get(cellIndex).getValue());
        }
        
        // TODO Quality check this...
        int size = resourceList.size();
        int next = 1;
        for (String resource : resourceList) {
            if (next < size) {
                switch (sortOrder) {
                case ASC:
                    assertTrue("Resources are not properly sorted (" + sortOrder + ")", 
                            resource.compareTo(resourceList.get(next)) <= 0);
                    break;
                default:
                    assertTrue("Resources are not properly sorted (" + sortOrder + ")", 
                            resource.compareTo(resourceList.get(next)) >= 0);
                    break;
                }
            }
            else break;
            next++;
        }
        
    }

}
