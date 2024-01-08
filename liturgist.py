import argparse
import json
import os
import sys
from datetime import datetime, timedelta

import pandas as pd
from pybars import Compiler
from weasyprint import HTML

csv_key_to_template_key = {
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


def parse_arguments():
    parser = argparse.ArgumentParser(description="A liturgical document generator")
    parser.add_argument("--date", help="A date on the schedule to select data for the template in"
                                       " MM/DD/YYYY. Defaults to next sunday if unspecified.")
    parser.add_argument("--print", help="Print selected data as JSON.", action="store_true")
    parser.add_argument("--template", help="A path to a mustache template")
    parser.add_argument("-o", "--output", dest="output_path", help="A path to the output file",
                        default="output/out.pdf")
    parser.add_argument("schedule", help="A path to a schedule - csv, ods, xlsx, and json are supported")

    return parser.parse_args()


def next_sunday():
    today = datetime.now().date()
    days_until_sunday = (6 - today.weekday()) % 7
    return today + timedelta(days=days_until_sunday)


def read_schedule(schedule_path):
    _, file_extension = os.path.splitext(schedule_path)

    if file_extension == ".csv":
        return pd.read_csv(schedule_path)
    elif file_extension == ".ods":
        return pd.read_excel(schedule_path, engine="odf")
    elif file_extension == ".xlsx" or file_extension == ".xls":
        return pd.read_excel(schedule_path)
    elif file_extension == ".json":
        return pd.read_json(schedule_path)
    else:
        raise ValueError(f"Unexpected schedule file type: {file_extension}")


def load_template_from_file(template_path):
    with open(template_path, 'r') as file:
        template_source = file.read()
        compiler = Compiler()
        template = compiler.compile(template_source)
        return template


def main():
    args = parse_arguments()

    if args.date is not None:
        date = datetime.strptime(args.date, "%m/%d/%y")
    else:
        date = next_sunday()

    if args.template is None and not args.print:
        print("You must specify a template file or --print.", file=sys.stderr)
        return

    try:
        schedule = read_schedule(args.schedule)
    except Exception as e:
        print("Error reading schedule:", e, file=sys.stderr)
        return

    week = schedule[schedule["Date"] == datetime.strftime(date, "%m/%d/%y")]

    if week.empty:
        print(f"Date {args.date} was not found in the schedule.", file=sys.stderr)
        return

    formatted_date = date.strftime("%A, %B %d, %Y")

    data = {
        "DATE": formatted_date,
        **{
            template_key: week[csv_key].iloc[0]
            for csv_key, template_key in csv_key_to_template_key.items()
            if csv_key in week
        }
    }

    if args.print:
        print(json.dumps(data))

    if args.template is not None:
        template = load_template_from_file(args.template)

        rendered_content = template(data)

        os.makedirs(os.path.dirname(args.output_path), exist_ok=True)

        _, file_extension = os.path.splitext(args.output_path)

        try:
            if file_extension == ".pdf":
                HTML(string=rendered_content).write_pdf(args.output_path)
            else:
                with open(args.output_path, 'w') as file:
                    file.write(rendered_content)

            print(f"{args.output_path} generated successfully")
        except Exception as e:
            print("Error generating:", e, file=sys.stderr)


if __name__ == "__main__":
    main()
