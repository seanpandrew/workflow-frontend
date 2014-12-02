var ptor    = protractor.getInstance();
var driver  = ptor.driver;

describe('stub creation', function() {

    var random =            Math.round(Math.random() * 1000000);
    var stubTitle =         ('Auto Test: ' + random);

    // Locators
    var expectedDate =      by.cssContainingText('.help-block','20:00 BST');
    var stub =              by.cssContainingText('li', stubTitle);
    var datePicker =        by.id("wfDateTimePickerText1");
    var saveStubButton =    by.partialButtonText('Save stub');
    var sectionDropdown =   by.model('selectedSection');
    var technologyOption =  by.cssContainingText('option', 'Technology');
    var newStubButton =     by.partialButtonText('New'); // or by css with content-list__button--new ng-scope
    var stubTitleField =    by.id("stub_title");


    it('should create a new stub', function() {
        element(newStubButton).click();
        element(stubTitleField).sendKeys(stubTitle);
        element(datePicker).sendKeys("8pm");
        browser.wait(function() {
           return ptor.isElementPresent(expectedDate);
       }, 8000);
        expect(ptor.isElementPresent(expectedDate)).toBeTruthy();
        element(saveStubButton).click();
        browser.driver.sleep(5000);
        element(sectionDropdown).click();
        element(technologyOption).click();
        ptor.waitForAngular();
        expect(ptor.isElementPresent(stub)).toBeTruthy();
  });
});


