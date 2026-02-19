# Word Report Templates Directory

## 📁 Template Location

Place your Word document templates (.docx files) in this directory:

```
performance-portal/backend/src/main/resources/templates/
```

## 📝 Template Files

### Default Template
- **File**: `technical_report_template.docx`
- **Purpose**: Standard technical report template
- **Usage**: Default template for Word report generation

### Custom Templates
You can add multiple templates for different report types:
- `executive_summary_template.docx` - Executive summary reports
- `detailed_analysis_template.docx` - Detailed analysis reports
- `comparison_template.docx` - Test comparison reports

## 🔧 How to Use Templates

### 1. Create Your Template

Create a Word document (.docx) with placeholders using double curly braces:

```
Performance Test Report
=======================

Test Name: {{test_name}}
Capability: {{capability_name}}
Test Date: {{test_date}}
Duration: {{test_duration}} minutes
Virtual Users: {{virtual_users}}

Performance Summary
-------------------
Average Response Time: {{avg_response_time}}
Throughput: {{throughput}}
Error Rate: {{error_rate}}
Total Requests: {{total_requests}}

Test Cases Summary
------------------
Total Test Cases: {{total_test_cases}}
Passed: {{test_cases_passed}}
Failed: {{test_cases_failed}}
Pass Rate: {{test_cases_pass_rate}}

{{test_cases_summary_table}}

System Architecture
-------------------
{{architecture_diagram}}

Transaction Analysis
--------------------
{{transaction_analysis_table}}
```

### 2. Save Template

Save your Word document in this directory:
```
backend/src/main/resources/templates/your_template_name.docx
```

### 3. Generate Report Using Template

**Via REST API:**
```bash
curl -X POST http://localhost:8080/api/v1/reports/generate/word/123 \
  -H "Content-Type: application/json" \
  -d '{"templateName": "your_template_name.docx"}'
```

**Via Java Code:**
```java
@Autowired
private WordReportGenerationService wordReportService;

Report report = wordReportService.generateWordReportFromTemplate(
    testRunId,
    "your_template_name.docx",
    "username"
);
```

## 📋 Available Placeholders

### Basic Information
- `{{capability_name}}` - Capability being tested
- `{{test_name}}` - Name of the test
- `{{test_date}}` - Date of test execution
- `{{test_duration}}` - Duration in minutes
- `{{virtual_users}}` - Number of virtual users
- `{{test_scenario}}` - Test scenario description
- `{{infrastructure_details}}` - Infrastructure configuration
- `{{description}}` - Test description
- `{{status}}` - Test run status
- `{{generated_date}}` - Report generation timestamp

### Performance Metrics
- `{{avg_response_time}}` - Average response time
- `{{min_response_time}}` - Minimum response time
- `{{max_response_time}}` - Maximum response time
- `{{throughput}}` - Requests per second
- `{{error_rate}}` - Error percentage
- `{{total_requests}}` - Total number of requests
- `{{std_deviation}}` - Standard deviation

### Test Cases
- `{{total_test_cases}}` - Total number of test cases
- `{{test_cases_passed}}` - Number of passed test cases
- `{{test_cases_failed}}` - Number of failed test cases
- `{{test_cases_pass_rate}}` - Pass rate percentage
- `{{has_test_cases}}` - Boolean flag (true/false)
- `{{test_cases_summary_table}}` - Placeholder for test cases table

### Architecture
- `{{has_architecture_diagram}}` - Boolean flag (true/false)
- `{{architecture_diagram}}` - Placeholder for diagram image

### Transactions
- `{{total_transactions}}` - Total number of transactions
- `{{slowest_transaction}}` - Name of slowest transaction
- `{{slowest_transaction_time}}` - Response time of slowest
- `{{transaction_analysis_table}}` - Placeholder for transaction table

## 🎨 Template Design Tips

### 1. Use Styles
- Apply Word styles (Heading 1, Heading 2, etc.) for consistent formatting
- Use built-in table styles for professional appearance

### 2. Placeholder Placement
- Place placeholders exactly where you want the value to appear
- For tables, place placeholder on its own line
- For images, place placeholder in a paragraph by itself

### 3. Formatting
- Format placeholder text as you want the replacement to appear
- Bold, italic, colors will be preserved for text replacements
- Tables and images will use default formatting

### 4. Page Layout
- Set margins, headers, and footers in the template
- Add page numbers if needed
- Include company logo in header/footer

## 📊 Table Placeholders

### Test Cases Table
When you use `{{test_cases_summary_table}}`, a table will be inserted with:
- Test Case ID
- Name
- Category
- Priority
- Status
- Execution Time

**Limit**: First 30 test cases

### Transaction Analysis Table
When you use `{{transaction_analysis_table}}`, a table will be inserted with:
- Transaction Name
- Average Response Time
- Min Response Time
- Max Response Time
- Throughput
- Error Count

**Limit**: Top 20 transactions by response time

## 🖼️ Image Placeholders

### Architecture Diagram
Place `{{architecture_diagram}}` on its own line where you want the diagram:

```
System Architecture
-------------------

{{architecture_diagram}}

The above diagram shows...
```

The image will be inserted with:
- Automatic format detection (PNG, JPG, SVG)
- Appropriate sizing (400x300 EMU by default)
- Centered alignment

## ⚠️ Important Notes

1. **File Format**: Only .docx files are supported (not .doc)
2. **Placeholder Format**: Must use exactly `{{placeholder_name}}` (double curly braces)
3. **Case Sensitive**: Placeholder names are case-sensitive
4. **No Spaces**: Don't add spaces inside braces: `{{ test_name }}` ❌ vs `{{test_name}}` ✅
5. **Template Size**: Keep templates under 5MB for best performance

## 🔍 Troubleshooting

### Template Not Found
**Error**: "Template not found: template_name.docx"
**Solution**: 
- Verify file is in `backend/src/main/resources/templates/`
- Check filename spelling and extension
- Rebuild project: `mvn clean package`

### Placeholders Not Replaced
**Error**: Placeholders appear as-is in generated report
**Solution**:
- Check placeholder format: `{{placeholder_name}}`
- Ensure no extra spaces or special characters
- Verify placeholder name matches available placeholders

### Image Not Inserted
**Error**: Architecture diagram placeholder remains
**Solution**:
- Verify architecture diagram was uploaded for the test run
- Check image file exists and is accessible
- Ensure image format is supported (PNG, JPG, SVG)

### Table Not Generated
**Error**: Table placeholder remains
**Solution**:
- Verify test cases or transactions exist for the test run
- Check placeholder is on its own line
- Ensure data is available in database

## 📦 Example Template Structure

```
┌─────────────────────────────────────┐
│  COMPANY LOGO          Page Header  │
├─────────────────────────────────────┤
│                                     │
│  Performance Test Report            │
│  ========================            │
│                                     │
│  Test: {{test_name}}                │
│  Date: {{test_date}}                │
│                                     │
│  Executive Summary                  │
│  ------------------                 │
│  {{description}}                    │
│                                     │
│  Performance Metrics                │
│  -------------------                │
│  Response Time: {{avg_response_time}}│
│  Throughput: {{throughput}}         │
│  Error Rate: {{error_rate}}         │
│                                     │
│  System Architecture                │
│  -------------------                │
│  {{architecture_diagram}}           │
│                                     │
│  Test Cases                         │
│  -----------                        │
│  {{test_cases_summary_table}}       │
│                                     │
│  Transactions                       │
│  ------------                       │
│  {{transaction_analysis_table}}     │
│                                     │
├─────────────────────────────────────┤
│  Footer Text          Page Numbers  │
└─────────────────────────────────────┘
```

## 🚀 Quick Start

1. **Copy your existing Word template** to this directory
2. **Add placeholders** where you want dynamic content
3. **Test the template** by generating a report
4. **Refine** based on the output

## 📞 Support

For template-related issues:
1. Check this README
2. Review the REPORT_GENERATION_GUIDE.md in project root
3. Check application logs for errors
4. Contact development team

---

**Last Updated**: 2026-02-11  
**Template Version**: 1.0