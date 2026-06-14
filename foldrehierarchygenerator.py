import os
import base64
import json

def create_self_extracting_script(source_folder, output_filename="unpacker.py"):
    if not os.path.exists(source_folder):
        print(f"Error: The folder '{source_folder}' does not exist.")
        return

    print(f"Packing folder: {source_folder}...")
    
    file_dictionary = {}
    
    # Walk through the directory tree
    for root, dirs, files in os.walk(source_folder):
        for file in files:
            file_path = os.path.join(root, file)
            # Get the relative path so the tree rebuilds correctly
            relative_path = os.path.relpath(file_path, source_folder)
            
            # Read the file as binary and encode it to Base64
            with open(file_path, "rb") as f:
                encoded_content = base64.b64encode(f.read()).decode('utf-8')
                file_dictionary[relative_path] = encoded_content
                print(f"  -> Packed: {relative_path}")

    # The code that will be written into the output file
    unpacker_template = f'''import os
import base64

# This dictionary contains all the files encoded in Base64
packed_data = {json.dumps(file_dictionary, indent=4)}

def extract_all(destination_folder="extracted_project"):
    print(f"Extracting files to '{{destination_folder}}'...")
    
    for relative_path, encoded_content in packed_data.items():
        # Figure out where this file belongs
        target_path = os.path.join(destination_folder, relative_path)
        
        # Create any necessary parent directories
        os.makedirs(os.path.dirname(target_path), exist_ok=True)
        
        # Decode the Base64 content and write it back as a file
        with open(target_path, "wb") as f:
            f.write(base64.b64decode(encoded_content))
            
        print(f"  -> Extracted: {{relative_path}}")
        
    print("\\nExtraction complete! Your folder is ready.")

if __name__ == "__main__":
    extract_all()
'''

    # Write the Unpacker script to the disk
    with open(output_filename, "w", encoding="utf-8") as out_file:
        out_file.write(unpacker_template)
        
    print(f"\nSuccess! Give '{output_filename}' to anyone. When they run it, it will recreate the folder.")

if __name__ == "__main__":
    folder_to_pack = input("Enter the path of the folder you want to pack: ")
    create_self_extracting_script(folder_to_pack)