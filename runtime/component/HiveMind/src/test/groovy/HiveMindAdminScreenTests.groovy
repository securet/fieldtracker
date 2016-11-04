/*
 * This software is in the public domain under CC0 1.0 Universal plus a
 * Grant of Patent License.
 *
 * To the extent possible under law, the author(s) have dedicated all
 * copyright and related and neighboring rights to this software to the
 * public domain worldwide. This software is distributed without any
 * warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication
 * along with this software (see the LICENSE.md file). If not, see
 * <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import org.moqui.screen.ScreenTest
import org.moqui.screen.ScreenTest.ScreenTestRender
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class HiveMindAdminScreenTests extends Specification {
    protected final static Logger logger = LoggerFactory.getLogger(HiveMindAdminScreenTests.class)

    @Shared
    ExecutionContext ec
    @Shared
    ScreenTest screenTest
    @Shared
    long effectiveTime = System.currentTimeMillis()

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.user.loginUser("john.doe", "moqui")
        screenTest = ec.screen.makeTest().baseScreenPath("apps/hmadmin")

        ec.entity.tempSetSequencedIdPrimary("mantle.party.communication.CommunicationEvent", 62100, 10)
        ec.entity.tempSetSequencedIdPrimary("mantle.work.effort.WorkEffort", 62100, 10)
    }

    def cleanupSpec() {
        long totalTime = System.currentTimeMillis() - screenTest.startTime
        logger.info("Rendered ${screenTest.renderCount} screens (${screenTest.errorCount} errors) in ${ec.l10n.format(totalTime/1000, "0.000")}s, output ${ec.l10n.format(screenTest.renderTotalChars/1000, "#,##0")}k chars")

        ec.entity.tempResetSequencedIdPrimary("mantle.party.communication.CommunicationEvent")
        ec.entity.tempResetSequencedIdPrimary("mantle.work.effort.WorkEffort")
        ec.destroy()
    }

    def setup() {
        ec.artifactExecution.disableAuthz()
    }

    def cleanup() {
        ec.artifactExecution.enableAuthz()
    }

    def "render HiveMind Admin screens with no required parameters"() {
        when:
        Set<String> screensToSkip = new HashSet()
        List<String> screenPaths = screenTest.getNoRequiredParameterPaths(screensToSkip)
        for (String screenPath in screenPaths) {
            ScreenTestRender str = screenTest.render(screenPath, null, null)
            logger.info("Rendered ${screenPath} in ${str.getRenderTime()}ms, ${str.output?.length()} characters")
        }

        then:
        screenTest.errorCount == 0
    }

    @Unroll
    def "render HiveMind Admin screen (#screenPath, #containsTextList)"() {
        setup:
        ScreenTestRender str = screenTest.render(screenPath, null, null)
        // logger.info("Rendered ${screenPath} in ${str.getRenderTime()}ms, output:\n${str.output}")
        boolean containsAll = true
        for (String containsText in containsTextList) {
            boolean contains = containsText ? str.assertContains(containsText) : true
            if (!contains) {
                logger.info("In ${screenPath} text [${containsText}] not found:\n${str.output}")
                containsAll = false
            }

        }

        expect:
        !str.errorMessages
        containsAll

        where:
        screenPath | containsTextList

        // Vendor
        "Vendor/EditVendor?partyId=ORG_ZIZI_SERVICES" |
                ['Ziziwork Services', 'Internal', 'payment.biziwork.services@test.com']
        "Vendor/EditProjects?partyId=ORG_ZIZI_SERVICES" | ['HM - HiveMind PM Build Out', 'Vendor - Bill From']
        "Vendor/EditUsers?partyId=ORG_ZIZI_SERVICES" | ['Developer', 'Manager']
        "Vendor/Accounting/AcctgPreference?partyId=ORG_ZIZI_SERVICES" |
                ['Ziziwork Industries', 'Clone Accounting Settings']

        // Client
        "Client/EditClient?partyId=ORG_ACME" |
                ['Another Company Making Everything', 'Accounts Payable', 'billing.acme@test.com']
        "Client/EditProjects?partyId=ORG_ACME" | ['HM - HiveMind PM Build Out', 'Customer - Bill To']
        "Client/EditUsers?partyId=ORG_ACME" | ['Acme', 'Client Manager']

        // User
        "User/EditUser?partyId=EX_JOHN_DOE" | ['john.doe@test.com', 'John']
        "User/EditProjects?partyId=EX_JOHN_DOE" | ['Manager', 'Programmer Lead', 'Assigned']
        "User/EditTasks?partyId=EX_JOHN_DOE" | ['HM-004', 'Dashboard My Tasks', 'Assigned']
        "User/EditRequests?partyId=EX_JOHN_DOE" | ['DEMO_001', 'Add Create Request on dashboard', 'Assignee']

        // Team
        "Team/EditTeam?partyId=ORG_ZIZI_DEVA" | ['Ziziwork Dev Team A']
        "Team/EditUsers?partyId=ORG_ZIZI_DEVA" | ['Developer', 'Assignee']

        // Rates
        "EditRateAmounts?partyId=EX_JOHN_DOE" | ['Standard', 'Client Billing', 'John Doe']

        // WikiSpace
        "WikiSpace/EditWikiSpace?wikiSpaceId=DEMO" | ['Demo Wiki Space', 'DEMO.md']

        // Project
        "Project/EditProject?workEffortId=HM" | ['HiveMind PM Build Out', 'In Progress', 'Budget $10,000.00']
        "Project/EditMilestones?rootWorkEffortId=HM" | ['HM Milestone 1', 'In Progress']
        "Project/EditUsers?rootWorkEffortId=HM" | ['john.doe', 'Ziziwork Services', 'Customer - Bill To']
        "Project/EditWikiPages?rootWorkEffortId=HM" | []

        // Accounting/Invoice
        "Accounting/Invoice/FindInvoice?statusId_op=in&statusId=InvoiceReceived,InvoiceApproved&toPartyId=ORG_ZIZI_RETAIL" |
                ['Ziddleman Incorporated', 'Ziziwork Retail', 'Sales/Purchase']
        "Accounting/Invoice/EditInvoice?invoiceId=55100" | ['Current: Approved', 'Unpaid $1,824.25', 'ORG_ZIZI_RETAIL']
        "Accounting/Invoice/EditInvoice?invoiceId=55400" |
                ['ORG_ZIZI_RETAIL', 'Current: Payment Sent', 'Applied Payments $23,830.00']
        "Accounting/Invoice/EditInvoiceItems?invoiceId=55400" | ['Demo Product One-One', 'Sales - Shipping and Handling']
        "Accounting/Invoice/PrintInvoice?invoiceId=55400&renderMode=xsl-fo" |
                ['1350 E. Flamingo Rd. #9876, Las Vegas, NV 89119-5263', 'Asset - Inventory']

        // Accounting/Payment
        "Accounting/Payment/EditPayment?paymentId=55400" |
                ['ORG_ZIZI_RETAIL', 'Applied $23,830.00', 'Current: Delivered']
        "Accounting/Payment/PaymentCheck?paymentIds=55400&renderMode=xsl-fo" | ['Ziddleman Incorporated',
                'Twenty three thousand eight hundred thirty and 00/100', 'Picker Bot 2000']
        "Accounting/Payment/PaymentDetail?paymentIds=55400&renderMode=xsl-fo" |
                ['Ziddleman Incorporated', '$23,830.00', 'Picker Bot 2000']

        // Accounting Other
        "Accounting/FinancialAccount/EditFinancialAccount?finAccountId=55700" |
                ['Ziziwork Retail', 'Joe Public', 'Current: Active']
        "Accounting/FinancialAccount/FinancialAccountTrans?finAccountId=55700" |
                ['Customer Service Credit', 'Ziziwork Retail ']
        "Accounting/Transaction/EditTransaction?acctgTransId=55700" |
                ['Joe Public', '430000000', 'CUSTOMER SERVICE CREDITS']
        "Accounting/GlAccount/EditGlAccount?glAccountId=110000000" | ['CASH AND EQUIVALENT ASSET', 'Ziziwork Industries']

        // Accounting/Reports
        "Accounting/Reports/BalanceSheet?organizationPartyId=ORG_ZIZI_RETAIL&timePeriodIdList=55100&detail=true" |
                ["121000000: ACCOUNTS RECEIVABLE", "210000000: ACCOUNTS PAYABLE"]
        "Accounting/Reports/IncomeStatement?organizationPartyId=ORG_ZIZI_RETAIL&timePeriodIdList=55100&detail=true" |
                ["411000000: PRODUCT SALES", "614200000: NETWORK CHARGES"]
        "Accounting/Reports/CashFlowStatement?organizationPartyId=ORG_ZIZI_RETAIL&timePeriodIdList=55100&detail=true" |
                ["111100000: GENERAL CHECKING ACCOUNT", "182000000: ACCUMULATED DEPRECIATION - EQUIPMENT"]
        "Accounting/Reports/RetainedEarningsStatement?organizationPartyId=ORG_ZIZI_RETAIL&timePeriodIdList=55100" |
                ["Net Earnings", "Ziziwork Retail Fiscal"]
        "Accounting/Reports/FinancialRatios?organizationPartyId=ORG_ZIZI_RETAIL&timePeriodIdList=55100" |
                ["Total Assets", "Accounts Receivable"]

        "Accounting/Reports/PostedAmountSummary?organizationPartyId=ORG_ZIZI_RETAIL&dateRange_poffset=0&dateRange_period=Year" |
                ["ACCOUNTS PAYABLE", "DEPRECIATION - EQUIPMENT"]
        "Accounting/Reports/PostedBalanceSummary?organizationPartyId=ORG_ZIZI_RETAIL&timePeriodId=55100" |
                ["CUSTOMER SERVICE CREDITS (Contra Revenue)", "NET INCOME (Income)"]
    }
}
