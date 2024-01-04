import sys

csvKeyToTemplateKey = {
    "Hymn 1": "HYMN_1",
    "Hymn 2": "HYMN_2",
    "Hymn 3": "HYMN_3",
    "Hymn 4": "HYMN_4",
    "Hymn 5": "HYMN_5",
    "Hymn 6": "HYMN_6",
    "Hymn 7": "HYMN_7",
    "Scripture": "SCRIPTURE",
    "Prayer Verse": "PRAYER_VERSE",
    "Assurance Verse": "ASSURANCE_VERSE",
    "Catechism Question": "CATECHISM_QUESTION",
    "Catechism Answer": "CATECHISM_ANSWER",
    "Catechism Scripture References": "CATECHISM_SCRIPTURE",
    "Benediction": "BENEDICTION",
    "Benediction Scripture": "BENEDICTION_SCRIPTURE",
    "OT Reading": "OT_READING",
    "NT Reading": "NT_READING",
    "Baptisms": "BAPTISMS",
}

usage = f"""
    liturgist <options> template.html 
    
    A pdf generator for liturgical events.
    
    --date:     A date on the schedule to select data for the template replacement

    --help: Print usage

    -o --output (optional): A path to the output pdf (default path is $DEFAULT_OUTPUT_FILE)
    
    --template: A path to a moustache template
                Template names: {[value for value in csvKeyToTemplateKey]}

    --schedule: A path to a schedule - csv, json, and xlsx are supported
                Column names: ${[key for key in csvKeyToTemplateKey]}

"""

def main():
	print(usage)

if __name__ == "__main__":
	sys.exit(main())