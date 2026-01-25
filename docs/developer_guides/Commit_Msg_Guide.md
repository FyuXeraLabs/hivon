# Commit Message Guide

**Strictly** follow the **these guideline**. **Always** remember to **pull the latest changes** before making any modifications. **Create your own branch** (for example: john-patch or john) for future pull request creation. Refer to all relevant documents, especially the **Project Document** and the **Work Plan Document**. You can identify the files assigned to you in the **File Assignment Summary Document**.

## Format
```
type(scope): message #issue_no
- Optional bullet points
- More details if needed
```

## Types & When to Use

**feat** – New feature or user-facing functionality
```
feat(inventory): add bulk item import #45 #48
- Support CSV and Excel upload
- Add validation for SKU duplicates
```

**fix** – Bug fixes
```
fix(stock): correct inventory count sync #67
```

**style** – UI/visual changes (no logic changes)
```
style(dashboard): improve warehouse layout view #23
```

**assets** – Images, fonts, media files
```
assets: add warehouse floor plan images #89
```

**db** – Database changes
```
db: create inventory_transactions table #34
```

**chore** – Configuration, builds, dependencies
```
chore: update barcode scanner library #12
```

**refactor** – Code restructuring (no behavior change)
```
refactor: optimize inventory search service #33
```

**test** – Testing changes
```
test(receiving): add goods receipt tests #44
```

**docs** – Documentation
```
docs: update warehouse setup guide #22
```

**hotfix** – Critical production fixes
```
hotfix: fix shipment creation error #101
```

**logic** – Business logic changes
```
logic(picking): validate FIFO rules #55
```

**api** – API endpoints
```
api: add stock level endpoint #77
```

## Best Practices
- Start with lowercase
- Keep first line under 72 chars
- Reference issues with # (issue number is optional for follow-up commits, but **required for the initial commit of the file**). **Putting issue number is recommended!** 
- Use bullet points for complex changes
- Write in present tense ("add" not "added")
- Be specific about what changed

**Finding Issue Numbers**: Issue numbers for each task are mentioned in **development guides** and **file assignment summary** documentation.

## Examples

```
feat(orders): support batch picking #15
- Group orders by warehouse zone
- Generate optimized pick lists
```

```
fix(reporting): handle empty stock movements #28
```

```
refactor(adjustments): streamline adjustments workflow #39
```

---

**Important Note:** When committing a file for the first time as part of a new feature or fix, **you must include the issue number**. Follow-up commits for the same feature/fix may skip the issue number if appropriate. **Putting issue number is recommended!**