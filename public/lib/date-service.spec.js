
describe('lib/date-service', function() {

  before(function(done) {

    System.import('lib/date-service')
      .then(function() {
        done();
      })
      .catch(done);

  });


  beforeEach(function() {
    angular.mock.module('wfDateService');
  });


  describe('wfDateParser.parseDate()', function() {
    it('should parse text inputs relative to timezone when in LON', function() {

      angular.mock.inject(function(wfDateParser) {

        // 6am on 14 August 2014 in London local time (BST +0100)
        var parsed = wfDateParser.parseDate('14-08-2014 6:00', 'LON');

        expect(parsed).to.be.an.instanceof(Date);
        expect(parsed.toISOString()).to.eql('2014-08-14T05:00:00.000Z');
      });
    });


    it('should parse text inputs relative to timezone when in NYC', function() {

      angular.mock.inject(function(wfDateParser) {

        // 6am on 14 August 2014 in New York local time (EDT -0400)
        var parsed = wfDateParser.parseDate('08-14-2014 6:00', 'NYC');

        expect(parsed).to.be.an.instanceof(Date);
        expect(parsed.toISOString()).to.eql('2014-08-14T10:00:00.000Z');
      });
    });


    it('should parse text inputs relative to timezone when in SYD', function() {

      angular.mock.inject(function(wfDateParser) {

        // 6am on 14 August 2014 in Sydney local time (EST +1000)
        var parsed = wfDateParser.parseDate('14-08-2014 6am', 'SYD');

        expect(parsed).to.be.an.instanceof(Date);
        expect(parsed.toISOString()).to.eql('2014-08-13T20:00:00.000Z');
      });
    });

  });

  // integration tests to parse a date range
  describe('wfDateParser.parseRange()', function() {

    it('should parse range for today in LON', function() {

      angular.mock.inject(function(wfDateParser) {

        sinon.stub(wfDateParser, 'now').returns(new Date('2014-10-07T06:48:32Z'));

        var parsed = wfDateParser.parseRange('today', 'LON');

        expect(parsed).not.to.be.null;
        expect(parsed.from).to.be.an.instanceof(Date);
        expect(parsed.until).to.be.an.instanceof(Date);

        // 2014-10-11 is BST +0100
        expect(parsed.from.toISOString()).to.eql('2014-10-06T23:00:00.000Z');
        expect(parsed.until.toISOString()).to.eql('2014-10-07T23:00:00.000Z');
      });

    });


    it('should parse range for tomorrow in LON', function() {

      angular.mock.inject(function(wfDateParser) {

        sinon.stub(wfDateParser, 'now').returns(new Date('2014-10-07T06:48:32Z'));

        var parsed = wfDateParser.parseRange('tomorrow', 'LON');

        expect(parsed).not.to.be.null;
        expect(parsed.from).to.be.an.instanceof(Date);
        expect(parsed.until).to.be.an.instanceof(Date);

        // 2014-10-11 is BST +0100
        expect(parsed.from.toISOString()).to.eql('2014-10-07T23:00:00.000Z');
        expect(parsed.until.toISOString()).to.eql('2014-10-08T23:00:00.000Z');
      });

    });


    it('should parse range for the weekend in LON', function() {

      angular.mock.inject(function(wfDateParser) {

        sinon.stub(wfDateParser, 'now').returns(new Date('2014-10-07T06:48:32Z'));

        var parsed = wfDateParser.parseRange('weekend', 'LON');

        expect(parsed).not.to.be.null;
        expect(parsed.from).to.be.an.instanceof(Date);
        expect(parsed.until).to.be.an.instanceof(Date);

        // 2014-10-11 is BST +0100
        expect(parsed.from.toISOString()).to.eql('2014-10-10T23:00:00.000Z');
        expect(parsed.until.toISOString()).to.eql('2014-10-12T23:00:00.000Z');
      });

    });


    it('should parse range for tomorrow in NYC', function() {

      angular.mock.inject(function(wfDateParser) {

        sinon.stub(wfDateParser, 'now').returns(new Date('2014-10-07T06:48:32Z'));

        var parsed = wfDateParser.parseRange('tomorrow', 'NYC');

        expect(parsed).not.to.be.null;
        expect(parsed.from).to.be.an.instanceof(Date);
        expect(parsed.until).to.be.an.instanceof(Date);

        // 2014-10-11 is EDT -0400
        expect(parsed.from.toISOString()).to.eql('2014-10-08T04:00:00.000Z');
        expect(parsed.until.toISOString()).to.eql('2014-10-09T04:00:00.000Z');
      });

    });


    it('should parse range for tomorrow in SYD', function() {

      angular.mock.inject(function(wfDateParser) {

        sinon.stub(wfDateParser, 'now').returns(new Date('2014-10-07T06:48:32Z'));

        var parsed = wfDateParser.parseRange('tomorrow', 'SYD');

        expect(parsed).not.to.be.null;
        expect(parsed.from).to.be.an.instanceof(Date);
        expect(parsed.until).to.be.an.instanceof(Date);

        // 2014-10-11 is +1100
        expect(parsed.from.toISOString()).to.eql('2014-10-07T13:00:00.000Z');
        expect(parsed.until.toISOString()).to.eql('2014-10-08T13:00:00.000Z');
      });

    });

  });
});
