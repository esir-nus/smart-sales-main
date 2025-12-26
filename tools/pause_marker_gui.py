#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Pause Marker GUI - Tkinter interface for pause_marker.py

Provides a simple GUI to configure and run the pause marker tool.
"""

import tkinter as tk
from tkinter import ttk, filedialog, messagebox, scrolledtext
import subprocess
import sys
import re
from pathlib import Path


class PauseMarkerGUI:
    def __init__(self, root):
        self.root = root
        self.root.title("Pause Marker Tool - Side-by-Side Comparison")
        # Larger window for better text visibility
        self.root.geometry("1600x950")
        # Allow window to be resized
        self.root.minsize(1200, 600)
        
        # Variables
        self.json_path = tk.StringVar()
        self.boundary_short_ms_var = tk.StringVar(value="250")
        self.color_mode = tk.StringVar(value="ansi")
        self.speaker_prefix = tk.StringVar(value="S")
        self.out_dir = tk.StringVar()
        self.title_text = tk.StringVar(value="Pause Marker Experiment")
        self.config_visible = tk.BooleanVar(value=True)
        
        self.setup_ui()
    
    def setup_ui(self):
        # Main container with paned window for resizable panels
        main_paned = ttk.PanedWindow(self.root, orient=tk.HORIZONTAL)
        main_paned.pack(fill=tk.BOTH, expand=True, padx=5, pady=5)
        
        # Left pane: Configuration (collapsible)
        config_frame = ttk.Frame(main_paned, width=300)
        main_paned.add(config_frame, weight=0)
        
        # Config header with toggle
        config_header = ttk.Frame(config_frame)
        config_header.pack(fill=tk.X, pady=(0, 5))
        ttk.Label(config_header, text="Configuration", font=("Arial", 11, "bold")).pack(side=tk.LEFT)
        ttk.Checkbutton(config_header, text="Show", variable=self.config_visible, 
                       command=self.toggle_config).pack(side=tk.RIGHT)
        
        # Config content frame
        self.config_content = ttk.Frame(config_frame)
        self.config_content.pack(fill=tk.BOTH, expand=True)
        
        # Compact config layout
        row = 0
        ttk.Label(self.config_content, text="JSON File:").grid(row=row, column=0, sticky=tk.W, pady=3)
        ttk.Entry(self.config_content, textvariable=self.json_path, width=35).grid(row=row, column=1, sticky=(tk.W, tk.E), padx=3)
        ttk.Button(self.config_content, text="...", command=self.browse_json, width=3).grid(row=row, column=2, padx=2)
        self.config_content.columnconfigure(1, weight=1)
        row += 1
        
        ttk.Label(self.config_content, text="Boundary Short (ms):").grid(row=row, column=0, sticky=tk.W, pady=3)
        boundary_entry = ttk.Entry(self.config_content, textvariable=self.boundary_short_ms_var, width=35)
        boundary_entry.grid(row=row, column=1, columnspan=2, sticky=(tk.W, tk.E), padx=3)
        row += 1
        
        # Help text for boundary short
        help_label = ttk.Label(self.config_content, 
                              text="Mark boundary as suspicious if gap_ms < this threshold (default: 250ms)",
                              font=("Arial", 8), foreground="gray")
        help_label.grid(row=row, column=0, columnspan=3, sticky=tk.W, padx=3, pady=(0, 5))
        row += 1
        
        ttk.Label(self.config_content, text="Color:").grid(row=row, column=0, sticky=tk.W, pady=3)
        color_frame = ttk.Frame(self.config_content)
        color_frame.grid(row=row, column=1, columnspan=2, sticky=tk.W, padx=3)
        for mode in ["none", "ansi", "html"]:
            ttk.Radiobutton(color_frame, text=mode, variable=self.color_mode, value=mode, width=6).pack(side=tk.LEFT)
        row += 1
        
        ttk.Label(self.config_content, text="Prefix:").grid(row=row, column=0, sticky=tk.W, pady=3)
        ttk.Entry(self.config_content, textvariable=self.speaker_prefix, width=10).grid(row=row, column=1, sticky=tk.W, padx=3)
        row += 1
        
        ttk.Label(self.config_content, text="Out Dir:").grid(row=row, column=0, sticky=tk.W, pady=3)
        ttk.Entry(self.config_content, textvariable=self.out_dir, width=30).grid(row=row, column=1, sticky=(tk.W, tk.E), padx=3)
        ttk.Button(self.config_content, text="...", command=self.browse_out_dir, width=3).grid(row=row, column=2, padx=2)
        row += 1
        
        # Buttons
        button_frame = ttk.Frame(self.config_content)
        button_frame.grid(row=row, column=0, columnspan=3, pady=10, sticky=(tk.W, tk.E))
        ttk.Button(button_frame, text="▶ Run", command=self.run_tool).pack(side=tk.LEFT, padx=2, fill=tk.X, expand=True)
        ttk.Button(button_frame, text="Clear", command=self.clear_output).pack(side=tk.LEFT, padx=2, fill=tk.X, expand=True)
        ttk.Button(button_frame, text="Exit", command=self.root.quit).pack(side=tk.LEFT, padx=2, fill=tk.X, expand=True)
        
        # Right pane: Comparison area (maximized)
        comparison_container = ttk.Frame(main_paned)
        main_paned.add(comparison_container, weight=1)
        comparison_container.columnconfigure(0, weight=1)
        comparison_container.columnconfigure(1, weight=1)
        comparison_container.rowconfigure(1, weight=1)
        
        # Header with labels
        header_frame = ttk.Frame(comparison_container)
        header_frame.grid(row=0, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=(0, 5))
        
        control_header = ttk.Label(header_frame, text="CONTROL (Baseline)", 
                                  font=("Arial", 12, "bold"), background="#e8f4f8", 
                                  relief=tk.RAISED, padding=5)
        control_header.pack(side=tk.LEFT, fill=tk.BOTH, expand=True, padx=(0, 2))
        
        experiment_header = ttk.Label(header_frame, text="EXPERIMENT (With Markers)", 
                                     font=("Arial", 12, "bold"), background="#fff4e8", 
                                     relief=tk.RAISED, padding=5)
        experiment_header.pack(side=tk.LEFT, fill=tk.BOTH, expand=True, padx=(2, 0))
        
        # Text areas with monospace font - NO WRAP to prevent cutoff
        font_family = "Courier" if sys.platform != "darwin" else "Monaco"
        font_size = 10
        
        # Create text widgets with both vertical and horizontal scrolling
        # Control panel (left)
        control_frame = ttk.Frame(comparison_container)
        control_frame.grid(row=1, column=0, sticky=(tk.W, tk.E, tk.N, tk.S), padx=(0, 2))
        control_frame.columnconfigure(0, weight=1)
        control_frame.rowconfigure(0, weight=1)
        
        # Text widget with word wrapping to fit visible area
        self.control_text = tk.Text(control_frame, 
                                    wrap=tk.WORD,  # Word wrap to fit visible width
                                    font=(font_family, font_size), 
                                    bg="#f8f8f8", 
                                    relief=tk.SUNKEN, 
                                    borderwidth=2,
                                    undo=False,
                                    maxundo=0,
                                    padx=5,
                                    pady=5,
                                    spacing1=2,  # Top spacing for paragraphs
                                    spacing2=12,  # Line spacing to prevent character cutoff
                                    spacing3=2)  # Bottom spacing for paragraphs
        control_v_scroll = ttk.Scrollbar(control_frame, orient=tk.VERTICAL, command=self.control_text.yview)
        self.control_text.configure(yscrollcommand=control_v_scroll.set)
        
        self.control_text.grid(row=0, column=0, sticky=(tk.W, tk.E, tk.N, tk.S))
        control_v_scroll.grid(row=0, column=1, sticky=(tk.N, tk.S))
        
        # Experiment panel (right)
        experiment_frame = ttk.Frame(comparison_container)
        experiment_frame.grid(row=1, column=1, sticky=(tk.W, tk.E, tk.N, tk.S), padx=(2, 0))
        experiment_frame.columnconfigure(0, weight=1)
        experiment_frame.rowconfigure(0, weight=1)
        
        self.experiment_text = tk.Text(experiment_frame, 
                                       wrap=tk.WORD,  # Word wrap to fit visible width
                                       font=(font_family, font_size),
                                       bg="#fffef8", 
                                       relief=tk.SUNKEN, 
                                       borderwidth=2,
                                       undo=False,
                                       maxundo=0,
                                       padx=5,
                                       pady=5,
                                       spacing1=2,  # Top spacing for paragraphs
                                       spacing2=12,  # Line spacing to prevent character cutoff
                                       spacing3=2)  # Bottom spacing for paragraphs
        experiment_v_scroll = ttk.Scrollbar(experiment_frame, orient=tk.VERTICAL, command=self.experiment_text.yview)
        self.experiment_text.configure(yscrollcommand=experiment_v_scroll.set)
        
        self.experiment_text.grid(row=0, column=0, sticky=(tk.W, tk.E, tk.N, tk.S))
        experiment_v_scroll.grid(row=0, column=1, sticky=(tk.N, tk.S))
        
        # Configure text tags for colors (for markers)
        self.setup_color_tags()
        
        # Status bar at bottom
        status_frame = ttk.Frame(self.root)
        status_frame.pack(fill=tk.X, side=tk.BOTTOM)
        self.status_text = tk.StringVar(value="Ready")
        status_label = ttk.Label(status_frame, textvariable=self.status_text, relief=tk.SUNKEN, anchor=tk.W, padding=3)
        status_label.pack(fill=tk.X)
        
        
    def browse_json(self):
        filename = filedialog.askopenfilename(
            title="Select JSON File",
            filetypes=[("JSON files", "*.json"), ("All files", "*.*")]
        )
        if filename:
            self.json_path.set(filename)
    
    def browse_out_dir(self):
        dirname = filedialog.askdirectory(title="Select Output Directory")
        if dirname:
            self.out_dir.set(dirname)
    
    
    def setup_color_tags(self):
        """Setup color tags for pause markers"""
        # Color scheme matching pause_marker.py
        colors = {
            0: ('#6b7280', 'white'),      # gray
            1: ('#0891b2', 'white'),      # cyan
            2: ('#ca8a04', '#ffffcc'),    # yellow with light yellow bg
            3: ('#a855f7', '#ffffcc'),    # magenta with light yellow bg
            4: ('#ef4444', '#ffffcc'),    # red with light yellow bg
        }
        
        # Configure all tags upfront
        for pause_class, (fg_color, bg_color) in colors.items():
            tag_name = f"pause_P{pause_class}_bg"
            try:
                self.experiment_text.tag_config(tag_name, 
                                               foreground=fg_color,
                                               background=bg_color,
                                               font=("Courier", 10, "bold"))
            except Exception:
                # Fallback if tag config fails
                self.experiment_text.tag_config(tag_name, foreground=fg_color, font=("Courier", 10, "bold"))
    
    def toggle_config(self):
        """Toggle configuration panel visibility"""
        if self.config_visible.get():
            self.config_content.pack(fill=tk.BOTH, expand=True)
        else:
            self.config_content.pack_forget()
    
    
    def clear_output(self):
        self.control_text.delete(1.0, tk.END)
        self.experiment_text.delete(1.0, tk.END)
        self.status_text.set("Ready")
    
    def set_status(self, message):
        self.status_text.set(message)
        self.root.update()
    
    def run_tool(self):
        if not self.json_path.get():
            messagebox.showerror("Error", "Please select a JSON file")
            return
        
        json_file = Path(self.json_path.get())
        if not json_file.exists():
            messagebox.showerror("Error", f"File not found: {json_file}")
            return
        
        # Build command - use 'ansi' color mode to get colored output
        script_path = Path(__file__).parent / "pause_marker.py"
        cmd = [
            sys.executable,
            str(script_path),
            str(json_file),
            "--boundary-short-ms", self.boundary_short_ms_var.get(),
            "--color", "ansi",  # Use 'ansi' to get color codes we can parse
            "--speaker-prefix", self.speaker_prefix.get(),
        ]
        
        if self.out_dir.get():
            cmd.extend(["--out-dir", self.out_dir.get()])
        
        # Clear output
        self.clear_output()
        self.set_status("Running...")
        
        try:
            # Run the tool
            process = subprocess.Popen(
                cmd,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                text=True,
                bufsize=1,
                universal_newlines=True
            )
            
            # Collect all output
            full_output = []
            for line in process.stdout:
                full_output.append(line)
            
            process.wait()
            
            if process.returncode == 0:
                # Parse output to separate EXPERIMENT and CONTROL
                output_text = ''.join(full_output)
                self.parse_and_display(output_text)
                self.set_status("✓ Completed successfully!")
                if self.out_dir.get():
                    self.set_status(f"✓ Completed! Output files written to: {self.out_dir.get()}")
            else:
                error_text = ''.join(full_output)
                self.control_text.insert(1.0, f"Error: Process exited with code {process.returncode}\n\n{error_text}")
                self.set_status(f"✗ Process exited with code {process.returncode}")
                
        except Exception as e:
            messagebox.showerror("Error", f"Failed to run tool: {e}")
            self.control_text.insert(1.0, f"Error: {e}\n")
            self.set_status(f"Error: {e}")
    
    def strip_ansi_codes(self, text):
        """Remove ANSI escape codes from text"""
        ansi_escape = re.compile(r'\x1B(?:[@-Z\\-_]|\[[0-?]*[ -/]*[@-~])')
        return ansi_escape.sub('', text)
    
    def parse_and_display(self, output_text):
        """Parse the output and display CONTROL and EXPERIMENT side by side with colors"""
        # Split by the separator markers
        lines = output_text.split('\n')
        
        experiment_lines = []
        control_lines = []
        current_section = None
        
        for line in lines:
            if "=== EXPERIMENT ===" in line:
                current_section = "experiment"
                continue
            elif "=== CONTROL ===" in line:
                current_section = "control"
                continue
            
            if current_section == "experiment":
                experiment_lines.append(line)
            elif current_section == "control":
                control_lines.append(line)
        
        # Clear text areas
        self.experiment_text.delete(1.0, tk.END)
        self.control_text.delete(1.0, tk.END)
        
        # Display control (clean, no colors)
        control_text = '\n'.join(control_lines).strip()
        if control_text:
            self.control_text.delete(1.0, tk.END)
            self.control_text.insert(1.0, control_text)
        else:
            self.control_text.insert(1.0, "No control output found")
        
        # Display experiment with colored markers
        experiment_text = '\n'.join(experiment_lines).strip()
        if experiment_text:
            self.experiment_text.delete(1.0, tk.END)
            self.insert_colored_text(self.experiment_text, experiment_text)
        else:
            self.experiment_text.insert(1.0, "No experiment output found")
        
        # Scroll both to top
        self.control_text.see(1.0)
        self.experiment_text.see(1.0)
        
        # Force update to ensure proper rendering
        self.root.update_idletasks()
    
    def insert_colored_text(self, text_widget, text):
        """Insert text with colored markers based on marker patterns"""
        try:
            # First strip all ANSI codes
            clean_text = self.strip_ansi_codes(text)
            
            # Pattern to match markers: sus{n}_P{class} <{gap}>
            # More flexible pattern to catch variations (with or without underscore)
            marker_pattern = re.compile(r'(sus\d+_?P(\d+)\s*<[^>]+>)')
            
            # Split text into segments
            last_pos = 0
            matches = list(marker_pattern.finditer(clean_text))
            
            # Build text with tags in one pass to avoid multiple insertions
            segments = []
            for match in matches:
                # Add text before marker
                if match.start() > last_pos:
                    plain_text = clean_text[last_pos:match.start()]
                    if plain_text:
                        segments.append((plain_text, None))
                
                # Add colored marker
                marker_text = match.group(1)
                try:
                    pause_class = int(match.group(2))
                    if 0 <= pause_class <= 4:
                        tag_name = f"pause_P{pause_class}_bg"
                        # Ensure tag exists
                        if tag_name not in text_widget.tag_names():
                            colors = {
                                0: ('#6b7280', 'white'),
                                1: ('#0891b2', 'white'),
                                2: ('#ca8a04', '#ffffcc'),
                                3: ('#a855f7', '#ffffcc'),
                                4: ('#ef4444', '#ffffcc'),
                            }
                            fg, bg = colors.get(pause_class, ('#000000', 'white'))
                            text_widget.tag_config(tag_name, foreground=fg, background=bg,
                                                  font=("Courier", 10, "bold"))
                        segments.append((marker_text, tag_name))
                    else:
                        segments.append((marker_text, None))
                except (ValueError, IndexError):
                    segments.append((marker_text, None))
                
                last_pos = match.end()
            
            # Add remaining text
            if last_pos < len(clean_text):
                plain_text = clean_text[last_pos:]
                if plain_text:
                    segments.append((plain_text, None))
            
            # Insert all segments at once (more efficient, less deadlock risk)
            for text_seg, tag in segments:
                if tag:
                    text_widget.insert(tk.END, text_seg, tag)
                else:
                    text_widget.insert(tk.END, text_seg)
                    
        except Exception as e:
            # Fallback: insert text without colors if anything fails
            clean_text = self.strip_ansi_codes(text)
            text_widget.insert(tk.END, clean_text)
            self.set_status(f"Warning: Color parsing failed: {e}")


def main():
    root = tk.Tk()
    app = PauseMarkerGUI(root)
    root.mainloop()


if __name__ == "__main__":
    main()

